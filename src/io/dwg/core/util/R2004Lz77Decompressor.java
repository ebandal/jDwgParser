package io.dwg.core.util;

/**
 * R2004/R2007 LZ77 Decompression
 * Based on libredwg decompress_r2007() function from decode_r2007.c
 */
public class R2004Lz77Decompressor {

    public byte[] decompress(byte[] compressed, int expectedSize) throws Exception {
        byte[] output = new byte[expectedSize];
        int srcPos = 0;
        int dstPos = 0;
        int length = 0;

        // Read first opcode
        if (srcPos >= compressed.length) throw new Exception("No data");
        int opcode = compressed[srcPos++] & 0xFF;

        // Special handling for opcode & 0xF0 == 0x20
        if ((opcode & 0xF0) == 0x20) {
            srcPos += 2;
            if (srcPos > compressed.length) throw new Exception("EOF reading 0x20 opcode");
            length = compressed[srcPos++] & 0x07;
            if (length == 0) throw new Exception("Zero length at opcode 0x20");
        }

        // Main decompression loop
        while (srcPos < compressed.length) {
            // Copy literal bytes
            if (length == 0) {
                int[] result = readLiteralLength(compressed, srcPos, opcode);
                length = result[0];
                srcPos = result[1];
            }

            // Check bounds for literal copy
            if (dstPos + length > expectedSize) {
                throw new Exception("Destination overflow during literal copy");
            }
            if (srcPos + length > compressed.length) {
                // Partial copy on EOF
                int available = compressed.length - srcPos;
                System.arraycopy(compressed, srcPos, output, dstPos, available);
                dstPos += available;
                break;
            }

            System.arraycopy(compressed, srcPos, output, dstPos, length);
            dstPos += length;
            srcPos += length;
            length = 0;

            if (srcPos >= compressed.length) break;

            opcode = compressed[srcPos++] & 0xFF;
            int[] instrResult = readInstructions(compressed, srcPos, opcode, output, dstPos);
            int offset = instrResult[0];
            length = instrResult[1];
            opcode = instrResult[2];
            srcPos = instrResult[3];

            // Copy previously decompressed bytes
            while (true) {
                if (dstPos + length > expectedSize) {
                    throw new Exception("Destination overflow during reference copy");
                }
                if (offset > dstPos) {
                    throw new Exception("Invalid offset: " + offset + " > " + dstPos);
                }

                copyBytes(output, dstPos, length, offset);
                dstPos += length;

                length = opcode & 0x07;
                if (length != 0 || srcPos >= compressed.length) break;

                opcode = compressed[srcPos++] & 0xFF;
                if ((opcode >> 4) == 0) break;

                if ((opcode >> 4) == 0x0F) {
                    opcode &= 0x0F;
                }

                instrResult = readInstructions(compressed, srcPos, opcode, output, dstPos);
                offset = instrResult[0];
                length = instrResult[1];
                opcode = instrResult[2];
                srcPos = instrResult[3];
            }
        }

        return java.util.Arrays.copyOf(output, dstPos);
    }

    // Returns [length, newSrcPos]
    private int[] readLiteralLength(byte[] src, int srcPos, int opcode) {
        int length = opcode + 8;

        if (length == 0x17) {  // opcode == 0x0F
            if (srcPos >= src.length) return new int[]{length, srcPos};

            int n = src[srcPos++] & 0xFF;
            length += n;

            if (n == 0xFF) {
                while (srcPos + 1 < src.length) {
                    int b0 = src[srcPos++] & 0xFF;
                    int b1 = src[srcPos++] & 0xFF;
                    n = b0 | (b1 << 8);

                    length += n;
                    if (n != 0xFFFF) break;
                }
            }
        }

        return new int[]{length, srcPos};
    }

    // Returns [offset, length, opcode, newSrcPos]
    private int[] readInstructions(byte[] src, int srcPos, int opcode, byte[] output, int dstPos) {
        int offset = 0;
        int length = 0;
        int origOpcode = opcode;

        switch (opcode >> 4) {
            case 0:  // 0x00-0x0F
                length = (opcode & 0x0F) + 0x13;
                if (srcPos >= src.length) throw new RuntimeException("EOF in read_instructions");
                offset = src[srcPos++] & 0xFF;
                if (srcPos >= src.length) throw new RuntimeException("EOF in read_instructions");
                opcode = src[srcPos++] & 0xFF;
                System.out.printf("[DEBUG] Case 0: opcode1=0x%02X, byte1=0x%02X, opcode2=0x%02X\n", origOpcode, offset & 0xFF, opcode);
                length = (((opcode >> 3) & 0x10) + length) & 0xFFFF;
                offset = (((opcode & 0x78) << 5) + 1 + offset) & 0xFFFF;
                System.out.printf("[DEBUG] Case 0 result: length=%d, offset=%d, dstPos=%d\n", length, offset, dstPos);
                break;

            case 1:  // 0x10-0x1F
                length = (opcode & 0x0F) + 3;
                if (srcPos >= src.length) throw new RuntimeException("EOF in read_instructions");
                offset = src[srcPos++] & 0xFF;
                if (srcPos >= src.length) throw new RuntimeException("EOF in read_instructions");
                opcode = src[srcPos++] & 0xFF;
                offset = ((opcode & 0xF8) << 5) + 1 + offset;
                break;

            case 2:  // 0x20-0x2F
            case 3:  // 0x30-0x3F
                length = (opcode & 0x0F) + 3;
                if (srcPos + 1 >= src.length) throw new RuntimeException("EOF in read_instructions");
                offset = (src[srcPos++] & 0xFF) | ((src[srcPos++] & 0xFF) << 8);
                if (srcPos >= src.length) throw new RuntimeException("EOF in read_instructions");
                opcode = src[srcPos++] & 0xFF;
                break;

            case 4:  // 0x40-0x4F
            case 5:  // 0x50-0x5F
                length = (opcode & 0x0F) + 0x13;
                if (srcPos + 1 >= src.length) throw new RuntimeException("EOF in read_instructions");
                offset = (src[srcPos++] & 0xFF) | ((src[srcPos++] & 0xFF) << 8);
                if (srcPos >= src.length) throw new RuntimeException("EOF in read_instructions");
                opcode = src[srcPos++] & 0xFF;
                break;

            case 6:  // 0x60-0x6F
            case 7:  // 0x70-0x7F
                length = (opcode & 0x0F) + 3;
                if (srcPos + 3 >= src.length) throw new RuntimeException("EOF in read_instructions");
                offset = (src[srcPos++] & 0xFF) | ((src[srcPos++] & 0xFF) << 8) |
                        ((src[srcPos++] & 0xFF) << 16);
                opcode = src[srcPos++] & 0xFF;
                break;

            case 8:  // 0x80-0x8F
            case 9:  // 0x90-0x9F
                length = (opcode & 0x0F) + 0x13;
                if (srcPos + 3 >= src.length) throw new RuntimeException("EOF in read_instructions");
                offset = (src[srcPos++] & 0xFF) | ((src[srcPos++] & 0xFF) << 8) |
                        ((src[srcPos++] & 0xFF) << 16);
                opcode = src[srcPos++] & 0xFF;
                break;

            default:
                throw new RuntimeException("Invalid opcode nibble: " + (opcode >> 4));
        }

        return new int[]{offset, length, opcode, srcPos};
    }

    private void copyBytes(byte[] dst, int dstPos, int length, int offset) {
        int srcPos = dstPos - offset;
        for (int i = 0; i < length; i++) {
            dst[dstPos + i] = dst[srcPos + i];
        }
    }
}
