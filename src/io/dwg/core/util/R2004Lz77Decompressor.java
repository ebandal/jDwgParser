package io.dwg.core.util;

/**
 * R2004/R2007 LZ77 Decompression
 * Based on libredwg decompress_r2007() from decode_r2007.c
 *
 * The algorithm uses a nested loop structure:
 * - Outer loop: handles literal bytes and initiates reference+literal pairs
 * - Inner loop: processes reference+literal pairs until new literal-only opcode
 */
public class R2004Lz77Decompressor {

    public byte[] decompress(byte[] compressed, int expectedSize) throws Exception {
        byte[] output = new byte[expectedSize];
        int srcIdx = 0;
        int dstIdx = 0;
        int literalLength = 0;
        int offset = 0;

        // Read initial opcode
        if (srcIdx >= compressed.length) throw new Exception("No data in compressed stream");
        int opcode = compressed[srcIdx++] & 0xFF;

        // Special case: opcode & 0xF0 == 0x20 has special initial literal length
        if ((opcode & 0xF0) == 0x20) {
            srcIdx += 2;
            if (srcIdx >= compressed.length) throw new Exception("EOF after initial 0x20 opcode");
            literalLength = compressed[srcIdx++] & 0x07;
            if (literalLength == 0) throw new Exception("Zero initial literal length");
        }

        // Main decompression loop
        while (srcIdx < compressed.length && dstIdx < expectedSize) {
            // Step 1: Read literal length if needed
            if (literalLength == 0) {
                int[] result = readLiteralLength(compressed, srcIdx, opcode);
                literalLength = result[0];
                srcIdx = result[1];
            }

            // Step 2: Copy literal bytes
            if (srcIdx + literalLength > compressed.length) {
                // Partial literal on EOF
                int available = compressed.length - srcIdx;
                System.arraycopy(compressed, srcIdx, output, dstIdx, available);
                dstIdx += available;
                break;
            }
            if (dstIdx + literalLength > expectedSize) {
                throw new Exception("Destination overflow during literal copy");
            }

            System.arraycopy(compressed, srcIdx, output, dstIdx, literalLength);
            dstIdx += literalLength;
            srcIdx += literalLength;
            literalLength = 0;

            // Step 3: Check if we've reached end of data
            if (srcIdx >= compressed.length) break;

            // Step 4: Read next opcode
            opcode = compressed[srcIdx++] & 0xFF;

            // Step 5: Parse instruction (offset and copy length)
            int[] instrResult = readInstructions(compressed, srcIdx, opcode);
            offset = instrResult[0];
            literalLength = instrResult[1];
            opcode = instrResult[2];
            srcIdx = instrResult[3];

            // Step 6: Inner loop - process reference+literal pairs
            while (true) {
                // Step 6a: Copy reference bytes
                if (dstIdx + literalLength > expectedSize) {
                    throw new Exception("Destination overflow during reference copy");
                }
                if (offset > dstIdx) {
                    throw new Exception("Invalid offset: " + offset + " > " + dstIdx);
                }

                copyBytes(output, dstIdx, literalLength, offset);
                dstIdx += literalLength;

                // Step 6b: Extract literal length from opcode low bits for next iteration
                literalLength = (opcode & 0x07);

                // Step 6c: Break if we have literal bytes or reached EOF
                if (literalLength != 0 || srcIdx >= compressed.length) break;

                // Step 6d: Read next opcode
                opcode = compressed[srcIdx++] & 0xFF;

                // Step 6e: Break if this is a literal-only opcode (high nibble == 0)
                if ((opcode >> 4) == 0) break;

                // Step 6f: Normalize upper nibble if all 1s
                if ((opcode >> 4) == 0x0F) {
                    opcode &= 0x0F;
                }

                // Step 6g: Parse next instruction
                instrResult = readInstructions(compressed, srcIdx, opcode);
                offset = instrResult[0];
                literalLength = instrResult[1];
                opcode = instrResult[2];
                srcIdx = instrResult[3];
            }
        }

        return java.util.Arrays.copyOf(output, dstIdx);
    }

    /**
     * Read literal length from opcode
     * Returns [length, newSrcIdx]
     */
    private int[] readLiteralLength(byte[] src, int srcIdx, int opcode) {
        int length = opcode + 8;

        if (length == 0x17) {  // opcode == 0x0F
            if (srcIdx >= src.length) return new int[]{length, srcIdx};

            int n = src[srcIdx++] & 0xFF;
            length += n;

            if (n == 0xFF) {
                while (srcIdx + 1 < src.length) {
                    int b0 = src[srcIdx++] & 0xFF;
                    int b1 = src[srcIdx++] & 0xFF;
                    n = b0 | (b1 << 8);

                    length += n;
                    if (n != 0xFFFF) break;
                }
            }
        }

        return new int[]{length, srcIdx};
    }

    /**
     * Parse instruction and extract offset/length
     * Returns [offset, length, opcode, newSrcIdx]
     */
    private int[] readInstructions(byte[] src, int srcIdx, int opcode) {
        int offset = 0;
        int length = 0;

        switch (opcode >> 4) {
            case 0:  // 0x00-0x0F
                length = (opcode & 0x0F) + 0x13;
                if (srcIdx >= src.length) break;
                offset = src[srcIdx++] & 0xFF;
                if (srcIdx >= src.length) break;
                opcode = src[srcIdx++] & 0xFF;
                length = (((opcode >> 3) & 0x10) + length) & 0xFFFF;
                offset = (((opcode & 0x78) << 5) + 1 + offset) & 0xFFFF;
                break;

            case 1:  // 0x10-0x1F
                length = (opcode & 0x0F) + 3;
                if (srcIdx >= src.length) break;
                offset = src[srcIdx++] & 0xFF;
                if (srcIdx >= src.length) break;
                opcode = src[srcIdx++] & 0xFF;
                offset = ((opcode & 0xF8) << 5) + 1 + offset;
                break;

            case 2:  // 0x20-0x2F
            case 3:  // 0x30-0x3F
                length = (opcode & 0x0F) + 3;
                if (srcIdx + 1 >= src.length) break;
                offset = (src[srcIdx++] & 0xFF) | ((src[srcIdx++] & 0xFF) << 8);
                if (srcIdx >= src.length) break;
                opcode = src[srcIdx++] & 0xFF;
                break;

            case 4:  // 0x40-0x4F
            case 5:  // 0x50-0x5F
                length = (opcode & 0x0F) + 0x13;
                if (srcIdx + 1 >= src.length) break;
                offset = (src[srcIdx++] & 0xFF) | ((src[srcIdx++] & 0xFF) << 8);
                if (srcIdx >= src.length) break;
                opcode = src[srcIdx++] & 0xFF;
                break;

            case 6:  // 0x60-0x6F
            case 7:  // 0x70-0x7F
                length = (opcode & 0x0F) + 3;
                if (srcIdx + 3 >= src.length) break;
                offset = (src[srcIdx++] & 0xFF) | ((src[srcIdx++] & 0xFF) << 8) |
                        ((src[srcIdx++] & 0xFF) << 16);
                if (srcIdx >= src.length) break;
                opcode = src[srcIdx++] & 0xFF;
                break;

            case 8:  // 0x80-0x8F
            case 9:  // 0x90-0x9F
                length = (opcode & 0x0F) + 0x13;
                if (srcIdx + 3 >= src.length) break;
                offset = (src[srcIdx++] & 0xFF) | ((src[srcIdx++] & 0xFF) << 8) |
                        ((src[srcIdx++] & 0xFF) << 16);
                if (srcIdx >= src.length) break;
                opcode = src[srcIdx++] & 0xFF;
                break;

            default:
                // Invalid opcode nibble, treat as error
                break;
        }

        return new int[]{offset, length, opcode, srcIdx};
    }

    /**
     * Copy bytes from earlier in output (reverse reference)
     */
    private void copyBytes(byte[] dst, int dstIdx, int length, int offset) {
        int srcIdx = dstIdx - offset;
        for (int i = 0; i < length; i++) {
            dst[dstIdx + i] = dst[srcIdx + i];
        }
    }
}
