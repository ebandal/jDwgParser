package io.dwg.core.util;

/**
 * R2004 LZ77 Decompression
 * Direct port of libredwg decompress_R2004_section() from decode.c
 *
 * Reference: libredwg/src/decode.c (lines 1125-1331)
 */
public class R2004Lz77Decompressor {

    private byte[] src;
    private int srcIdx;
    private byte[] dst;
    private int dstIdx;

    public byte[] decompress(byte[] compressed, int expectedSize) throws Exception {
        this.src = compressed;
        this.srcIdx = 0;
        this.dst = new byte[expectedSize];
        this.dstIdx = 0;

        if (srcIdx > src.length) {
            return new byte[0];
        }

        // Read first opcode
        int opcode1 = readRC();

        // If (opcode1 & 0xF0) == 0, handle as initial literal run
        // copy_bytes returns the next byte as the new opcode
        if ((opcode1 & 0xF0) == 0) {
            int litLen = readLiteralLength(opcode1);
            opcode1 = copyBytes(litLen);
        }

        // Main decompression loop
        while (srcIdx < src.length && dstIdx < expectedSize && opcode1 != 0x11) {
            int compBytes = 0;
            int compOffset = 0;

            if (opcode1 < 0x10 || opcode1 >= 0x40) {
                // Mode 1: single byte offset (0x00-0x0F or 0x40-0xFF)
                compBytes = (opcode1 >> 4) - 1;
                int opcode2 = readRC();
                compOffset = (((opcode1 >> 2) & 3) | (opcode2 << 2)) + 1;
            } else if (opcode1 < 0x20) {
                // Mode 2: 0x12-0x1F (0x10, 0x11 handled elsewhere)
                compBytes = readCompressedBytes(opcode1, 7);
                int[] offsetRef = new int[]{(opcode1 & 8) << 11};
                opcode1 = twoByteOffset(0x4000, offsetRef);
                compOffset = offsetRef[0];
            } else {
                // Mode 3: 0x20+
                compBytes = readCompressedBytes(opcode1, 0x1F);
                int[] offsetRef = new int[]{0};
                opcode1 = twoByteOffset(1, offsetRef);
                compOffset = offsetRef[0];
            }

            // Copy previously output bytes
            int pos = dstIdx;
            int end = pos + compBytes;
            if (end >= dst.length) {
                compBytes = dst.length - pos;
                end = pos + compBytes;
                opcode1 = 0x11;
            }

            // Validate offset before copying
            if (compOffset < 0 || pos - compOffset < 0) {
                break;
            }

            // LZ77 run-length extension (handles overlapping refs)
            for (; pos < end; pos++) {
                dst[pos] = dst[pos - compOffset];
            }
            dstIdx = end;

            // Read "literal data" length from lower 2 bits of opcode1
            int litLength = opcode1 & 3;
            if (litLength == 0) {
                if (srcIdx >= src.length) break;
                opcode1 = readRC();
                if ((opcode1 & 0xF0) == 0) {
                    litLength = readLiteralLength(opcode1);
                }
            }

            if (litLength > 0) {
                if (dstIdx + litLength <= dst.length) {
                    opcode1 = copyBytes(litLength);
                } else {
                    break;
                }
            }
        }

        return java.util.Arrays.copyOf(dst, dstIdx);
    }

    /**
     * Read one byte from src (bit_read_RC equivalent).
     * Returns -1 on EOF but casts to 0xFF for safety.
     */
    private int readRC() {
        if (srcIdx >= src.length) return 0;
        return src[srcIdx++] & 0xFF;
    }

    /**
     * Copy lit_length bytes from src to dst, then read and return next byte.
     * Direct port of libredwg copy_bytes().
     */
    private int copyBytes(int litLength) {
        for (int i = 0; i < litLength; i++) {
            if (srcIdx >= src.length || dstIdx >= dst.length) break;
            dst[dstIdx++] = src[srcIdx++];
        }
        // copy_bytes returns the next byte as next opcode
        return readRC();
    }

    /**
     * Read R2004 encoded literal length.
     * Direct port of libredwg read_literal_length().
     *
     * If (opcode & 0xF) == 0:
     *   Accumulate 0xFF for each zero byte read
     *   Add 0xF + lastbyte (non-zero byte)
     * Result is lowbits + 3.
     */
    private int readLiteralLength(int opcode) {
        int lowbits = opcode & 0xF;
        if (lowbits == 0) {
            int lastbyte = 0;
            while (srcIdx < src.length) {
                lastbyte = src[srcIdx++] & 0xFF;
                if (lastbyte != 0) break;
                lowbits += 0xFF;
            }
            lowbits += 0xF + lastbyte;
        }
        return lowbits + 3;
    }

    /**
     * Read R2004 encoded number of compressed bytes.
     * Direct port of libredwg read_compressed_bytes().
     *
     * If (opcode & bits) == 0:
     *   Accumulate 0xFF for each zero byte read
     *   Add lastbyte + bits
     * Result is compressed_bytes + 2.
     */
    private int readCompressedBytes(int opcode, int bits) {
        int compressedBytes = opcode & bits;
        if (compressedBytes == 0) {
            int lastbyte = 0;
            while (srcIdx < src.length) {
                lastbyte = src[srcIdx++] & 0xFF;
                if (lastbyte != 0) break;
                compressedBytes += 0xFF;
            }
            compressedBytes += lastbyte + bits;
        }
        return compressedBytes + 2;
    }

    /**
     * Read R2004 two-byte offset.
     * Direct port of libredwg two_byte_offset().
     *
     * offset |= (firstByte >> 2)
     * offset |= secondByte << 6
     * offset += plus
     * Returns firstByte (becomes next opcode).
     */
    private int twoByteOffset(int plus, int[] offsetRef) {
        int firstByte = readRC();
        int secondByte = readRC();
        offsetRef[0] |= (firstByte >> 2);
        offsetRef[0] |= (secondByte << 6);
        offsetRef[0] += plus;
        return firstByte;
    }
}
