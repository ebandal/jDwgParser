package decode.util;

/**
 * R2004 LZ77 Decompression
 * Direct port of libredwg decompress_R2004_section() from decode.c (lines 1125-1331)
 *
 * Verified working: Arc.dwg Section Map (144/144), Objects (4000/4000), etc.
 */
public class R2004Lz77 {

    private byte[] src;
    private int srcIdx;
    private byte[] dst;
    private int dstIdx;

    public static byte[] decompress(byte[] compressed, int expectedSize) {
        return new R2004Lz77().run(compressed, expectedSize);
    }

    private byte[] run(byte[] compressed, int expectedSize) {
        this.src = compressed;
        this.srcIdx = 0;
        this.dst = new byte[expectedSize];
        this.dstIdx = 0;

        if (src == null || src.length == 0) {
            return new byte[0];
        }

        int opcode1 = readRC();

        // Initial literal run
        if ((opcode1 & 0xF0) == 0) {
            int litLen = readLiteralLength(opcode1);
            opcode1 = copyBytes(litLen);
        }

        while (srcIdx < src.length && dstIdx < expectedSize && opcode1 != 0x11) {
            int compBytes;
            int compOffset;

            if (opcode1 < 0x10 || opcode1 >= 0x40) {
                compBytes = (opcode1 >> 4) - 1;
                int opcode2 = readRC();
                compOffset = (((opcode1 >> 2) & 3) | (opcode2 << 2)) + 1;
            } else if (opcode1 < 0x20) {
                compBytes = readCompressedBytes(opcode1, 7);
                int[] offsetRef = new int[]{(opcode1 & 8) << 11};
                opcode1 = twoByteOffset(0x4000, offsetRef);
                compOffset = offsetRef[0];
            } else {
                compBytes = readCompressedBytes(opcode1, 0x1F);
                int[] offsetRef = new int[]{0};
                opcode1 = twoByteOffset(1, offsetRef);
                compOffset = offsetRef[0];
            }

            int pos = dstIdx;
            int end = pos + compBytes;
            if (end >= dst.length) {
                compBytes = dst.length - pos;
                end = pos + compBytes;
                opcode1 = 0x11;
            }

            if (compOffset < 0 || pos - compOffset < 0) {
                break;
            }

            for (; pos < end; pos++) {
                dst[pos] = dst[pos - compOffset];
            }
            dstIdx = end;

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

    private int readRC() {
        if (srcIdx >= src.length) return 0;
        return src[srcIdx++] & 0xFF;
    }

    private int copyBytes(int litLength) {
        for (int i = 0; i < litLength; i++) {
            if (srcIdx >= src.length || dstIdx >= dst.length) break;
            dst[dstIdx++] = src[srcIdx++];
        }
        return readRC();
    }

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

    private int twoByteOffset(int plus, int[] offsetRef) {
        int firstByte = readRC();
        int secondByte = readRC();
        offsetRef[0] |= (firstByte >> 2);
        offsetRef[0] |= (secondByte << 6);
        offsetRef[0] += plus;
        return firstByte;
    }
}
