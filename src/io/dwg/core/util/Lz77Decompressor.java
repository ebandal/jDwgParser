package io.dwg.core.util;

/**
 * R2007+ LZ77 Decompressor
 * Direct port of libredwg's decompress_r2007() from decode_r2007.c
 * OpenDesign Spec v5.4.1 §4.7 (R2007 uses same compression as R2004/R18)
 */
public class Lz77Decompressor {

    private byte[] src;
    private int srcPos;
    private byte[] dst;
    private int dstPos;

    /**
     * Decompress R2007 LZ77-compressed data
     *
     * @param compressed Source compressed data
     * @param expectedSize Expected decompressed size (e.g., 0x110 = 272 for R2007 header)
     * @return Decompressed data
     */
    public byte[] decompress(byte[] compressed, int expectedSize) throws Exception {
        this.src = compressed;
        this.srcPos = 0;
        this.dst = new byte[expectedSize];
        this.dstPos = 0;

        if (compressed.length < 2) {
            throw new Exception("Compressed data too short");
        }

        int dstEnd = expectedSize;
        int srcEnd = compressed.length;

        long length = 0;
        long offset = 0;

        // Read first opcode
        int opcode = src[srcPos++] & 0xFF;

        // Special case: opcode 0x20-0x2F (header literal)
        if ((opcode & 0xF0) == 0x20) {
            srcPos += 2;  // skip 2 bytes
            if (srcPos >= srcEnd) {
                throw new Exception("Header truncated");
            }
            length = src[srcPos++] & 0x07;  // literal length 0-7 (low 3 bits)
            if (length == 0) {
                throw new Exception("Decompression error: zero length");
            }
        }

        // Main outer loop
        while (srcPos < srcEnd) {
            // Read literal length if not yet set
            if (length == 0) {
                length = readLiteralLength(opcode);
            }

            // Validate bounds
            if (dstPos + length > dstEnd || srcPos + length > srcEnd) {
                // Truncate to available
                length = Math.min(dstEnd - dstPos, srcEnd - srcPos);
                if (length <= 0) break;
            }

            // Copy literal bytes with special byte-reversal rules (like libredwg's copy_compressed_bytes)
            copyCompressedBytes((int)length);
            length = 0;

            if (srcPos >= srcEnd) break;

            // Read back-reference instruction
            opcode = src[srcPos++] & 0xFF;
            int[] result = readInstructions(opcode);
            opcode = result[0];
            offset = result[1] & 0xFFFFFFFFL;
            length = result[2] & 0xFFFFFFFFL;

            // Inner back-reference loop
            while (true) {
                // Validate bounds
                if (dstPos + length > dstEnd) {
                    return dst;  // Truncate at end
                }
                if (offset > dstPos) {
                    throw new Exception("Decompression error: offset " + offset
                        + " > dstPos " + dstPos);
                }

                // Copy back-reference bytes
                copyBytes((int)offset, (int)length);

                // Set literal count from opcode's low 3 bits
                length = opcode & 7;

                if (length != 0 || srcPos >= srcEnd) {
                    break;  // Exit inner loop with literal count set
                }

                opcode = src[srcPos++] & 0xFF;

                if ((opcode >> 4) == 0) {
                    break;  // Low opcode: outer loop will handle literal length
                }

                // Special case: high nibble == 0xF, mask away high nibble
                if ((opcode >> 4) == 0x0F) {
                    opcode &= 0x0F;
                }

                // Decode another back-reference
                int[] result2 = readInstructions(opcode);
                opcode = result2[0];
                offset = result2[1] & 0xFFFFFFFFL;
                length = result2[2] & 0xFFFFFFFFL;
            }
        }

        return dst;
    }

    /**
     * Read literal length per libredwg's read_literal_length()
     * Spec §4.7 page 50
     */
    private int readLiteralLength(int opcode) throws Exception {
        int length = (opcode & 0xFF) + 8;

        if (length == 0x17) {  // opcode == 0x0F
            if (srcPos >= src.length) return length;
            int n = src[srcPos++] & 0xFF;
            length += n;

            if (n == 0xFF) {
                while (srcPos + 1 < src.length) {
                    int n2 = (src[srcPos] & 0xFF) | ((src[srcPos + 1] & 0xFF) << 8);
                    srcPos += 2;
                    length += n2;
                    if (n2 != 0xFFFF) break;
                }
            }
        }
        return length;
    }

    /**
     * Decode back-reference instruction per libredwg's read_instructions()
     * Returns: [new_opcode, offset, length]
     * Spec §4.7 page 53
     */
    private int[] readInstructions(int opcode) throws Exception {
        int caseNum = opcode >> 4;
        int length, offset;
        int newOpcode;

        switch (caseNum) {
        case 0:
            length = (opcode & 0xF) + 0x13;
            if (srcPos >= src.length) return new int[] { 0, 0, length };
            offset = src[srcPos++] & 0xFF;
            if (srcPos >= src.length) return new int[] { 0, offset, length };
            newOpcode = src[srcPos++] & 0xFF;
            length = ((newOpcode >> 3) & 0x10) + length;
            offset = ((newOpcode & 0x78) << 5) + 1 + offset;
            break;

        case 1:
            length = (opcode & 0xF) + 3;
            if (srcPos >= src.length) return new int[] { 0, 0, length };
            offset = src[srcPos++] & 0xFF;
            if (srcPos >= src.length) return new int[] { 0, offset, length };
            newOpcode = src[srcPos++] & 0xFF;
            offset = ((newOpcode & 0xF8) << 5) + 1 + offset;
            break;

        case 2:
            if (srcPos >= src.length) return new int[] { opcode, 0, 0 };
            offset = src[srcPos++] & 0xFF;
            if (srcPos >= src.length) return new int[] { opcode, offset, 0 };
            offset = ((src[srcPos++] & 0xFF) << 8) | offset;
            length = opcode & 0x07;

            if ((opcode & 0x08) == 0) {
                if (srcPos >= src.length) return new int[] { opcode, offset, length };
                newOpcode = src[srcPos++] & 0xFF;
                length = (newOpcode & 0xF8) + length;
            } else {
                offset++;
                if (srcPos >= src.length) return new int[] { opcode, offset, length };
                length = ((src[srcPos++] & 0xFF) << 3) + length;
                if (srcPos >= src.length) return new int[] { opcode, offset, length };
                newOpcode = src[srcPos++] & 0xFF;
                length = (((newOpcode & 0xF8) << 8) + length) + 0x100;
            }
            break;

        default:  // case 3+
            length = opcode >> 4;
            offset = opcode & 0x0F;
            if (srcPos >= src.length) return new int[] { 0, offset, length };
            newOpcode = src[srcPos++] & 0xFF;
            offset = (((newOpcode & 0xF8) << 1) + offset) + 1;
            break;
        }

        return new int[] { newOpcode, offset, length };
    }

    private void copyCompressedBytes(int length) {
        // Direct port of libredwg copy_compressed_bytes() from decode_r2007.c
        // Macros: copy_1(o)=1 byte; copy_2(o)=REVERSED 2 bytes; copy_3(o)=REVERSED 3 bytes;
        //         copy_4(o)=4 bytes straight; copy_8(o)=8 bytes straight;
        //         copy_16(o)=16 bytes with 8-byte half-swap
        while (length >= 32) {
            // copy_16(16): dst[0..7]=src[24..31], dst[8..15]=src[16..23]
            System.arraycopy(src, srcPos + 24, dst, dstPos, 8);
            System.arraycopy(src, srcPos + 16, dst, dstPos + 8, 8);
            dstPos += 16;
            // copy_16(0): dst[0..7]=src[8..15], dst[8..15]=src[0..7]
            System.arraycopy(src, srcPos + 8, dst, dstPos, 8);
            System.arraycopy(src, srcPos + 0, dst, dstPos + 8, 8);
            dstPos += 16;
            srcPos += 32;
            length -= 32;
        }

        switch (length) {
            case 0: break;
            case 1:
                dst[dstPos++] = src[srcPos];
                srcPos += 1; break;
            case 2:  // copy_2(0)
                dst[dstPos++] = src[srcPos + 1]; dst[dstPos++] = src[srcPos];
                srcPos += 2; break;
            case 3:  // copy_3(0)
                dst[dstPos++] = src[srcPos + 2]; dst[dstPos++] = src[srcPos + 1]; dst[dstPos++] = src[srcPos];
                srcPos += 3; break;
            case 4:  // copy_4(0)
                System.arraycopy(src, srcPos, dst, dstPos, 4); dstPos += 4;
                srcPos += 4; break;
            case 5:  // copy_1(4); copy_4(0)
                dst[dstPos++] = src[srcPos + 4];
                System.arraycopy(src, srcPos, dst, dstPos, 4); dstPos += 4;
                srcPos += 5; break;
            case 6:  // copy_1(5); copy_4(1); copy_1(0)
                dst[dstPos++] = src[srcPos + 5];
                System.arraycopy(src, srcPos + 1, dst, dstPos, 4); dstPos += 4;
                dst[dstPos++] = src[srcPos];
                srcPos += 6; break;
            case 7:  // copy_2(5); copy_4(1); copy_1(0)
                dst[dstPos++] = src[srcPos + 6]; dst[dstPos++] = src[srcPos + 5];
                System.arraycopy(src, srcPos + 1, dst, dstPos, 4); dstPos += 4;
                dst[dstPos++] = src[srcPos];
                srcPos += 7; break;
            case 8:  // copy_8(0)
                System.arraycopy(src, srcPos, dst, dstPos, 8); dstPos += 8;
                srcPos += 8; break;
            case 9:  // copy_1(8); copy_8(0)
                dst[dstPos++] = src[srcPos + 8];
                System.arraycopy(src, srcPos, dst, dstPos, 8); dstPos += 8;
                srcPos += 9; break;
            case 10:  // copy_1(9); copy_8(1); copy_1(0)
                dst[dstPos++] = src[srcPos + 9];
                System.arraycopy(src, srcPos + 1, dst, dstPos, 8); dstPos += 8;
                dst[dstPos++] = src[srcPos];
                srcPos += 10; break;
            case 11:  // copy_2(9); copy_8(1); copy_1(0)
                dst[dstPos++] = src[srcPos + 10]; dst[dstPos++] = src[srcPos + 9];
                System.arraycopy(src, srcPos + 1, dst, dstPos, 8); dstPos += 8;
                dst[dstPos++] = src[srcPos];
                srcPos += 11; break;
            case 12:  // copy_4(8); copy_8(0)
                System.arraycopy(src, srcPos + 8, dst, dstPos, 4); dstPos += 4;
                System.arraycopy(src, srcPos, dst, dstPos, 8); dstPos += 8;
                srcPos += 12; break;
            case 13:  // copy_1(12); copy_4(8); copy_8(0)
                dst[dstPos++] = src[srcPos + 12];
                System.arraycopy(src, srcPos + 8, dst, dstPos, 4); dstPos += 4;
                System.arraycopy(src, srcPos, dst, dstPos, 8); dstPos += 8;
                srcPos += 13; break;
            case 14:  // copy_1(13); copy_4(9); copy_8(1); copy_1(0)
                dst[dstPos++] = src[srcPos + 13];
                System.arraycopy(src, srcPos + 9, dst, dstPos, 4); dstPos += 4;
                System.arraycopy(src, srcPos + 1, dst, dstPos, 8); dstPos += 8;
                dst[dstPos++] = src[srcPos];
                srcPos += 14; break;
            case 15:  // copy_2(13); copy_4(9); copy_8(1); copy_1(0)
                dst[dstPos++] = src[srcPos + 14]; dst[dstPos++] = src[srcPos + 13];
                System.arraycopy(src, srcPos + 9, dst, dstPos, 4); dstPos += 4;
                System.arraycopy(src, srcPos + 1, dst, dstPos, 8); dstPos += 8;
                dst[dstPos++] = src[srcPos];
                srcPos += 15; break;
            case 16:  // copy_16(0)
                System.arraycopy(src, srcPos + 8, dst, dstPos, 8);
                System.arraycopy(src, srcPos, dst, dstPos + 8, 8); dstPos += 16;
                srcPos += 16; break;
            case 17:  // copy_8(9); copy_1(8); copy_8(0)
                System.arraycopy(src, srcPos + 9, dst, dstPos, 8); dstPos += 8;
                dst[dstPos++] = src[srcPos + 8];
                System.arraycopy(src, srcPos, dst, dstPos, 8); dstPos += 8;
                srcPos += 17; break;
            case 18:  // copy_1(17); copy_16(1); copy_1(0)
                dst[dstPos++] = src[srcPos + 17];
                System.arraycopy(src, srcPos + 9, dst, dstPos, 8);
                System.arraycopy(src, srcPos + 1, dst, dstPos + 8, 8); dstPos += 16;
                dst[dstPos++] = src[srcPos];
                srcPos += 18; break;
            case 19:  // copy_3(16); copy_16(0)
                dst[dstPos++] = src[srcPos + 18]; dst[dstPos++] = src[srcPos + 17]; dst[dstPos++] = src[srcPos + 16];
                System.arraycopy(src, srcPos + 8, dst, dstPos, 8);
                System.arraycopy(src, srcPos, dst, dstPos + 8, 8); dstPos += 16;
                srcPos += 19; break;
            case 20:  // copy_4(16); copy_16(0)
                System.arraycopy(src, srcPos + 16, dst, dstPos, 4); dstPos += 4;
                System.arraycopy(src, srcPos + 8, dst, dstPos, 8);
                System.arraycopy(src, srcPos, dst, dstPos + 8, 8); dstPos += 16;
                srcPos += 20; break;
            case 21:  // copy_1(20); copy_4(16); copy_16(0)
                dst[dstPos++] = src[srcPos + 20];
                System.arraycopy(src, srcPos + 16, dst, dstPos, 4); dstPos += 4;
                System.arraycopy(src, srcPos + 8, dst, dstPos, 8);
                System.arraycopy(src, srcPos, dst, dstPos + 8, 8); dstPos += 16;
                srcPos += 21; break;
            case 22:  // copy_2(20); copy_4(16); copy_16(0)
                dst[dstPos++] = src[srcPos + 21]; dst[dstPos++] = src[srcPos + 20];
                System.arraycopy(src, srcPos + 16, dst, dstPos, 4); dstPos += 4;
                System.arraycopy(src, srcPos + 8, dst, dstPos, 8);
                System.arraycopy(src, srcPos, dst, dstPos + 8, 8); dstPos += 16;
                srcPos += 22; break;
            case 23:  // copy_3(20); copy_4(16); copy_16(0)
                dst[dstPos++] = src[srcPos + 22]; dst[dstPos++] = src[srcPos + 21]; dst[dstPos++] = src[srcPos + 20];
                System.arraycopy(src, srcPos + 16, dst, dstPos, 4); dstPos += 4;
                System.arraycopy(src, srcPos + 8, dst, dstPos, 8);
                System.arraycopy(src, srcPos, dst, dstPos + 8, 8); dstPos += 16;
                srcPos += 23; break;
            case 24:  // copy_8(16); copy_16(0)
                System.arraycopy(src, srcPos + 16, dst, dstPos, 8); dstPos += 8;
                System.arraycopy(src, srcPos + 8, dst, dstPos, 8);
                System.arraycopy(src, srcPos, dst, dstPos + 8, 8); dstPos += 16;
                srcPos += 24; break;
            case 25:  // copy_8(17); copy_1(16); copy_16(0)
                System.arraycopy(src, srcPos + 17, dst, dstPos, 8); dstPos += 8;
                dst[dstPos++] = src[srcPos + 16];
                System.arraycopy(src, srcPos + 8, dst, dstPos, 8);
                System.arraycopy(src, srcPos, dst, dstPos + 8, 8); dstPos += 16;
                srcPos += 25; break;
            case 26:  // copy_1(25); copy_8(17); copy_1(16); copy_16(0)
                dst[dstPos++] = src[srcPos + 25];
                System.arraycopy(src, srcPos + 17, dst, dstPos, 8); dstPos += 8;
                dst[dstPos++] = src[srcPos + 16];
                System.arraycopy(src, srcPos + 8, dst, dstPos, 8);
                System.arraycopy(src, srcPos, dst, dstPos + 8, 8); dstPos += 16;
                srcPos += 26; break;
            case 27:  // copy_2(25); copy_8(17); copy_1(16); copy_16(0)
                dst[dstPos++] = src[srcPos + 26]; dst[dstPos++] = src[srcPos + 25];
                System.arraycopy(src, srcPos + 17, dst, dstPos, 8); dstPos += 8;
                dst[dstPos++] = src[srcPos + 16];
                System.arraycopy(src, srcPos + 8, dst, dstPos, 8);
                System.arraycopy(src, srcPos, dst, dstPos + 8, 8); dstPos += 16;
                srcPos += 27; break;
            case 28:  // copy_4(24); copy_8(16); copy_16(0)
                System.arraycopy(src, srcPos + 24, dst, dstPos, 4); dstPos += 4;
                System.arraycopy(src, srcPos + 16, dst, dstPos, 8); dstPos += 8;
                System.arraycopy(src, srcPos + 8, dst, dstPos, 8);
                System.arraycopy(src, srcPos, dst, dstPos + 8, 8); dstPos += 16;
                srcPos += 28; break;
            case 29:  // copy_1(28); copy_4(24); copy_8(16); copy_16(0)
                dst[dstPos++] = src[srcPos + 28];
                System.arraycopy(src, srcPos + 24, dst, dstPos, 4); dstPos += 4;
                System.arraycopy(src, srcPos + 16, dst, dstPos, 8); dstPos += 8;
                System.arraycopy(src, srcPos + 8, dst, dstPos, 8);
                System.arraycopy(src, srcPos, dst, dstPos + 8, 8); dstPos += 16;
                srcPos += 29; break;
            case 30:  // copy_2(28); copy_4(24); copy_8(16); copy_16(0)
                dst[dstPos++] = src[srcPos + 29]; dst[dstPos++] = src[srcPos + 28];
                System.arraycopy(src, srcPos + 24, dst, dstPos, 4); dstPos += 4;
                System.arraycopy(src, srcPos + 16, dst, dstPos, 8); dstPos += 8;
                System.arraycopy(src, srcPos + 8, dst, dstPos, 8);
                System.arraycopy(src, srcPos, dst, dstPos + 8, 8); dstPos += 16;
                srcPos += 30; break;
            case 31:  // copy_1(30); copy_4(26); copy_8(18); copy_16(2); copy_2(0)
                dst[dstPos++] = src[srcPos + 30];
                System.arraycopy(src, srcPos + 26, dst, dstPos, 4); dstPos += 4;
                System.arraycopy(src, srcPos + 18, dst, dstPos, 8); dstPos += 8;
                System.arraycopy(src, srcPos + 10, dst, dstPos, 8);
                System.arraycopy(src, srcPos + 2, dst, dstPos + 8, 8); dstPos += 16;
                dst[dstPos++] = src[srcPos + 1]; dst[dstPos++] = src[srcPos];
                srcPos += 31; break;
        }
    }

    private void copyBytes(int offset, int length) {
        // Copy back-reference bytes from already-decompressed data
        int srcOffset = dstPos - offset;
        for (int i = 0; i < length; i++) {
            dst[dstPos + i] = dst[srcOffset + i];
        }
        dstPos += length;
    }
}
