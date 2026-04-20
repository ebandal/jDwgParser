package io.dwg.core.util;

/**
 * R2004 LZ77 Decompression
 * Based on libredwg decompress_R2004_section() from decode.c
 *
 * Opcode structure:
 * - Bits 0-3: literal length code (-3)
 * - Bits 4-7: compressed length code (-2)
 * - Special: 0x11 = EOF marker
 */
public class R2004Lz77Decompressor {
    private static final boolean DEBUG = false;

    public byte[] decompress(byte[] compressed, int expectedSize) throws Exception {
        byte[] output = new byte[expectedSize];
        int srcIdx = 0;
        int dstIdx = 0;
        int iterations = 0;
        final int MAX_ITERATIONS = 100000;

        while (srcIdx < compressed.length && dstIdx < expectedSize && iterations < MAX_ITERATIONS) {
            iterations++;

            if (srcIdx >= compressed.length) break;

            // Read opcode
            int opcode = compressed[srcIdx++] & 0xFF;

            if (DEBUG && iterations <= 10) {
                System.out.printf("[R2004LZ77] Iter %d: opcode=0x%02X srcIdx=%d dstIdx=%d\n",
                    iterations, opcode, srcIdx - 1, dstIdx);
            }

            // Special case: opcode == 0x11 means EOF
            if (opcode == 0x11) {
                if (DEBUG) System.out.printf("[R2004LZ77] EOF at iteration %d, dstIdx=%d\n", iterations, dstIdx);
                break;
            }

            // Decode literal length from low nibble
            int literalLen = (opcode & 0x0F) + 3;
            if ((opcode & 0x0F) == 0x0F) {
                // Extended literal length
                while (srcIdx < compressed.length) {
                    int b = compressed[srcIdx++] & 0xFF;
                    literalLen += b;
                    if (b != 0xFF) break;
                }
            }

            // Copy literal bytes
            int litCopied = 0;
            for (int i = 0; i < literalLen && srcIdx < compressed.length && dstIdx < expectedSize; i++) {
                output[dstIdx++] = compressed[srcIdx++];
                litCopied++;
            }

            if (DEBUG && iterations <= 10) {
                System.out.printf("[R2004LZ77] Literal: len=%d copied=%d dstIdx=%d\n",
                    literalLen, litCopied, dstIdx);
            }

            if (dstIdx >= expectedSize || srcIdx >= compressed.length) break;

            // Decode compressed length from high nibble
            int compLen = ((opcode >> 4) & 0x0F) + 2;
            if (((opcode >> 4) & 0x0F) == 0) {
                // Extended compressed length
                while (srcIdx < compressed.length) {
                    int b = compressed[srcIdx++] & 0xFF;
                    compLen += b;
                    if (b != 0xFF) break;
                }
            }

            // Read 2-byte offset
            if (srcIdx + 1 >= compressed.length) {
                if (DEBUG) System.out.printf("[R2004LZ77] Not enough bytes for offset at srcIdx=%d\n", srcIdx);
                break;
            }
            int b0 = compressed[srcIdx++] & 0xFF;
            int b1 = compressed[srcIdx++] & 0xFF;
            int compOffset = ((b0 | (b1 << 8)) >> 2) + 1;

            // Cap compLen to output space remaining
            if (dstIdx + compLen > expectedSize) {
                compLen = expectedSize - dstIdx;
            }

            // Validate offset
            if (compOffset <= 0 || compOffset > dstIdx) {
                if (DEBUG) System.out.printf("[R2004LZ77] Invalid offset %d at dstIdx=%d\n", compOffset, dstIdx);
                break;
            }

            // Copy referenced bytes (handles overlapping with source offset)
            int srcOff = dstIdx - compOffset;
            for (int i = 0; i < compLen; i++) {
                output[dstIdx++] = output[srcOff + i];
            }

            if (DEBUG && iterations <= 10) {
                System.out.printf("[R2004LZ77] Compressed: len=%d offset=%d dstIdx=%d\n",
                    compLen, compOffset, dstIdx);
            }
        }

        if (iterations >= MAX_ITERATIONS) {
            System.out.printf("[WARN] R2004Lz77: Max iterations reached, stopping at dstIdx=%d\n", dstIdx);
        }

        return java.util.Arrays.copyOf(output, dstIdx);
    }
}
