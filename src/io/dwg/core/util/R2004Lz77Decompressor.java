package io.dwg.core.util;

/**
 * R2004 LZ77 Decompression
 * Based on libredwg decompress_R2004_section() from decode.c
 *
 * Algorithm:
 * - Each iteration reads opcode + literal + compressed reference
 * - opcode encodes: literal_length and compressed_length/offset
 */
public class R2004Lz77Decompressor {

    public byte[] decompress(byte[] compressed, int expectedSize) throws Exception {
        byte[] output = new byte[expectedSize];
        int srcIdx = 0;
        int dstIdx = 0;

        while (srcIdx < compressed.length && dstIdx < expectedSize) {
            // Read opcode
            int opcode = compressed[srcIdx++] & 0xFF;

            // Special case: opcode == 0x11 means EOF
            if (opcode == 0x11) {
                break;
            }

            // Extract literal length from low nibble
            int literalLen = (opcode & 0x0F) + 3;

            // If literalLen == 18 (0x0F + 3), read extended length
            if ((opcode & 0x0F) == 0x0F) {
                while (srcIdx < compressed.length) {
                    int extLen = compressed[srcIdx++] & 0xFF;
                    literalLen += extLen;
                    if (extLen != 0xFF) break;
                }
            }

            // Copy literals
            if (dstIdx + literalLen > expectedSize) {
                literalLen = expectedSize - dstIdx;
            }
            if (srcIdx + literalLen > compressed.length) {
                literalLen = compressed.length - srcIdx;
            }
            System.arraycopy(compressed, srcIdx, output, dstIdx, literalLen);
            srcIdx += literalLen;
            dstIdx += literalLen;

            if (dstIdx >= expectedSize) break;
            if (srcIdx >= compressed.length) break;

            // Extract compressed length and offset from high nibble
            int compLen = ((opcode & 0xF0) >> 4) + 2;
            int compOffset = 0;

            if ((opcode & 0x0F) < 0x10) {  // Always true (0x00-0x0F), but kept for clarity
                // Read 2-byte offset
                if (srcIdx + 1 >= compressed.length) break;
                int b0 = compressed[srcIdx++] & 0xFF;
                int b1 = compressed[srcIdx++] & 0xFF;
                compOffset = (b1 << 8) | b0;
                compOffset += 1;
            }

            // Cap compLen to output space remaining
            if (dstIdx + compLen > expectedSize) {
                compLen = expectedSize - dstIdx;
            }

            // Validate offset
            if (compOffset <= 0 || compOffset > dstIdx) {
                // Invalid offset - stop decompression
                break;
            }

            // Copy referenced bytes
            int srcOff = dstIdx - compOffset;
            for (int i = 0; i < compLen; i++) {
                output[dstIdx + i] = output[srcOff + i];
            }
            dstIdx += compLen;
        }

        return java.util.Arrays.copyOf(output, dstIdx);
    }
}
