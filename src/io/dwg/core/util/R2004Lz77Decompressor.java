package io.dwg.core.util;

/**
 * R2004 LZ77 Decompression
 * Based on libredwg decompress_R2004_section() from decode.c
 *
 * Different from R2007 algorithm - simpler structure
 */
public class R2004Lz77Decompressor {

    public byte[] decompress(byte[] compressed, int expectedSize) throws Exception {
        byte[] output = new byte[expectedSize];
        int srcIdx = 0;
        int dstIdx = 0;
        int literalLength;
        int compBytes;
        int compOffset;
        int opcode1;

        // Read first opcode
        if (srcIdx >= compressed.length) throw new Exception("Empty compressed data");
        opcode1 = compressed[srcIdx++] & 0xFF;

        // If opcode & 0xF0 == 0, handle as literal length opcode
        if ((opcode1 & 0xF0) == 0) {
            int[] result = readLiteralLength(compressed, srcIdx, opcode1);
            literalLength = result[0];
            srcIdx = result[1];
            srcIdx = copyLiterals(compressed, srcIdx, literalLength, output, dstIdx);
            dstIdx += literalLength;
            if (srcIdx >= compressed.length) return java.util.Arrays.copyOf(output, dstIdx);
            opcode1 = compressed[srcIdx++] & 0xFF;
        }

        // Main decompression loop
        while (srcIdx < compressed.length && dstIdx < expectedSize && opcode1 != 0x11) {
            compBytes = 0;
            compOffset = 0;

            if ((opcode1 < 0x10 || opcode1 >= 0x40)) {
                // Single byte offset method: 0x00-0x0F or 0x40-0xFF
                compBytes = (opcode1 >> 4) - 1;
                if (srcIdx >= compressed.length) break;
                int opcode2 = compressed[srcIdx++] & 0xFF;
                compOffset = (((opcode1 >> 2) & 3) | (opcode2 << 2)) + 1;
            } else if (opcode1 < 0x20) {
                // Two-byte offset: 0x10-0x1F
                compBytes = readCompressedBytes(compressed, srcIdx, opcode1, 7);
                srcIdx += bytesNeeded(opcode1, 7);
                compOffset = (opcode1 & 8) << 11;
                int[] result = twoByteOffset(compressed, srcIdx, 0x4000, compOffset);
                opcode1 = result[0];
                srcIdx = result[1];
                compOffset = result[2];
            } else {
                // Two-byte offset: 0x20-0x3F
                compBytes = readCompressedBytes(compressed, srcIdx, opcode1, 0x1F);
                srcIdx += bytesNeeded(opcode1, 0x1F);
                int[] result = twoByteOffset(compressed, srcIdx, 1, 0);
                opcode1 = result[0];
                srcIdx = result[1];
                compOffset = result[2];
            }

            // Copy referenced bytes
            if (dstIdx + compBytes > expectedSize) {
                // Cap at decompressed size
                compBytes = expectedSize - dstIdx;
            }
            if (compOffset > dstIdx) {
                // Invalid offset - this may indicate corrupted data or an unsupported pattern
                // For now, return partial decompression
                System.out.printf("[WARN] R2004Lz77: Invalid offset %d > %d at srcIdx=%d, returning partial decomp (%d bytes)\n",
                    compOffset, dstIdx, srcIdx, dstIdx);
                return java.util.Arrays.copyOf(output, dstIdx);
            }

            copyBytes(output, dstIdx, compBytes, compOffset);
            dstIdx += compBytes;

            // Get next opcode
            if (srcIdx >= compressed.length) break;
            opcode1 = compressed[srcIdx++] & 0xFF;

            // Handle literal length from opcode low bits
            if ((opcode1 & 0xF0) == 0) {
                int[] result = readLiteralLength(compressed, srcIdx, opcode1);
                literalLength = result[0];
                srcIdx = result[1];
                if (srcIdx + literalLength > compressed.length) {
                    literalLength = compressed.length - srcIdx;
                }
                if (dstIdx + literalLength > expectedSize) {
                    literalLength = expectedSize - dstIdx;
                }
                System.arraycopy(compressed, srcIdx, output, dstIdx, literalLength);
                srcIdx += literalLength;
                dstIdx += literalLength;
                if (srcIdx >= compressed.length) break;
                opcode1 = compressed[srcIdx++] & 0xFF;
            }
        }

        return java.util.Arrays.copyOf(output, dstIdx);
    }

    private int[] readLiteralLength(byte[] src, int srcIdx, int opcode) {
        int length = opcode + 3;  // 0x01-0x0E: length + 3

        if ((opcode & 0x0F) == 0) {
            // 0x00: variable length
            int total = 0x0F;
            while (srcIdx < src.length) {
                int n = src[srcIdx++] & 0xFF;
                total += n;
                if (n != 0xFF) break;
            }
            length = total + 3;
        }

        return new int[]{length, srcIdx};
    }

    private int readCompressedBytes(byte[] src, int srcIdx, int opcode, int mask) {
        // Extract length from opcode
        int length = (opcode & mask) + 2;
        return length;
    }

    private int bytesNeeded(int opcode, int mask) {
        // For two-byte offset, we need to calculate how many bytes were used
        // For now, return 0 as it's handled in twoByteOffset
        return 0;
    }

    /**
     * Read two-byte offset
     * Returns [newOpcode, newSrcIdx, offset]
     */
    private int[] twoByteOffset(byte[] src, int srcIdx, int plus, int compOffset) {
        if (srcIdx + 1 >= src.length) {
            return new int[]{0, srcIdx, compOffset};
        }

        int b0 = src[srcIdx++] & 0xFF;
        int b1 = src[srcIdx++] & 0xFF;
        int offset = b0 | (b1 << 8);

        if (srcIdx >= src.length) {
            return new int[]{0, srcIdx, offset + plus};
        }

        int newOpcode = src[srcIdx++] & 0xFF;
        return new int[]{newOpcode, srcIdx, offset + plus};
    }

    private int copyLiterals(byte[] src, int srcIdx, int length, byte[] dst, int dstIdx) {
        int copied = 0;
        while (copied < length && srcIdx < src.length && dstIdx + copied < dst.length) {
            dst[dstIdx + copied] = src[srcIdx++];
            copied++;
        }
        return srcIdx;
    }

    private void copyBytes(byte[] dst, int dstIdx, int length, int offset) {
        int srcIdx = dstIdx - offset;
        for (int i = 0; i < length; i++) {
            dst[dstIdx + i] = dst[srcIdx + i];
        }
    }
}
