package io.dwg.core.util;

import java.util.Arrays;

/**
 * Decompressor for the LZ77 variant used by R2004 and later (R2004, R2010, R2013, R2018) data and
 * system pages.
 *
 * <p>Written from the Open Design Specification for .dwg files, section 4.7 "Compression".
 *
 * <p>The stream is: a Literal Length and that many literal bytes, then repeated groups of
 * (opcode1, back reference, literal run) until opcode 0x11. Each group copies {@code length}
 * bytes from {@code distance} bytes back in the output, then copies {@code litCount} bytes from
 * the input.
 *
 * <p>Notes on reading the specification:
 * <ul>
 *   <li>The offsets given in the specification are one less than the distance back:
 *       a copy starts at {@code outputPosition - (offset + 1)}.</li>
 *   <li>For opcodes 0x10–0x1F, bit 3 of opcode1 is bit 14 of the offset and the low three bits
 *       give the length. For 0x10–0x17 this is the same as the specification's
 *       "(opcode1 &amp; 0x0F) + 2" and "Long Compression Offset + 9".</li>
 * </ul>
 *
 * <p>Malformed input never throws: decoding stops and the bytes produced so far are returned.
 */
public class R2004Lz77Decompressor {

    private static final int END_OF_STREAM = 0x11;

    private byte[] in;
    private int inPos;
    private byte[] out;
    private int outPos;

    /**
     * Decompresses {@code compressed}.
     *
     * @param compressed   the compressed page data
     * @param expectedSize the size of the output buffer; decoding stops when it is full
     * @return the decompressed bytes, at most {@code expectedSize} long
     */
    public byte[] decompress(byte[] compressed, int expectedSize) {
        in = compressed;
        inPos = 0;
        io.dwg.core.io.ReadLimits.reserve((long) (Math.max(0, expectedSize)), "R2004Lz77Decompressor");
        out = new byte[Math.max(0, expectedSize)];
        outPos = 0;

        if (in.length == 0) {
            return new byte[0];
        }

        // The first group has no back reference, only a literal run.
        int opcode1 = nextByte();
        if ((opcode1 & 0xF0) == 0) {
            copyLiterals(literalLength(opcode1));
            opcode1 = nextByte();
        }

        while (opcode1 != END_OF_STREAM && opcode1 >= 0 && outPos < out.length) {
            int length;
            int offset;
            int litCount;

            if (opcode1 >= 0x40) {
                length = (opcode1 >> 4) - 1;
                int opcode2 = nextByte();
                if (opcode2 < 0) break;
                offset = (opcode2 << 2) | ((opcode1 & 0x0C) >> 2);
                litCount = opcode1 & 0x03;
            } else if (opcode1 >= 0x20) {
                length = opcode1 & 0x1F;
                if (length == 0) {
                    length = longCompressionOffset() + 0x1F;
                }
                length += 2;
                int first = nextByte();
                int second = nextByte();
                if (second < 0) break;
                offset = (first >> 2) | (second << 6);
                litCount = first & 0x03;
            } else if (opcode1 >= 0x10) {
                length = opcode1 & 0x07;
                if (length == 0) {
                    length = longCompressionOffset() + 0x07;
                }
                length += 2;
                int first = nextByte();
                int second = nextByte();
                if (second < 0) break;
                offset = ((first >> 2) | (second << 6)) + 0x3FFF + ((opcode1 & 0x08) << 11);
                litCount = first & 0x03;
            } else {
                // 0x00–0x0F cannot start a group.
                break;
            }

            if (!copyBack(offset + 1, length)) break;

            if (litCount == 0) {
                // The next byte is either a Literal Length or, if its high nibble is set,
                // the next opcode1 with a literal length of zero.
                int next = nextByte();
                if (next < 0) break;
                if ((next & 0xF0) != 0) {
                    opcode1 = next;
                    continue;
                }
                litCount = literalLength(next);
            }
            copyLiterals(litCount);
            opcode1 = nextByte();
        }

        return outPos == out.length ? out : Arrays.copyOf(out, outPos);
    }

    /** Returns the next input byte (0–255), or -1 at the end of the input. */
    private int nextByte() {
        return inPos < in.length ? in[inPos++] & 0xFF : -1;
    }

    /**
     * Literal Length, where {@code first} has a zero high nibble. A value of 1–15 means
     * value + 3. Zero starts a running total of 0x0F: each further 0x00 adds 0xFF, and the first
     * non-zero byte is added and ends it; 3 is then added.
     */
    private int literalLength(int first) {
        if (first != 0) {
            return first + 3;
        }
        return runningTotal(0x0F) + 3;
    }

    /**
     * Long Compression Offset: a non-zero byte is used as is. Zero starts a running total of
     * 0xFF: each further 0x00 adds 0xFF, and the first non-zero byte is added and ends it.
     */
    private int longCompressionOffset() {
        int b = nextByte();
        if (b != 0) {
            return Math.max(b, 0);
        }
        return runningTotal(0xFF);
    }

    private int runningTotal(int start) {
        long total = start;
        int b;
        while ((b = nextByte()) == 0) {
            total += 0xFF;
        }
        if (b > 0) {
            total += b;
        }
        return (int) Math.min(total, Integer.MAX_VALUE);
    }

    /** Copies {@code count} bytes from the input to the output, as far as both allow. */
    private void copyLiterals(int count) {
        int n = Math.min(count, Math.min(in.length - inPos, out.length - outPos));
        System.arraycopy(in, inPos, out, outPos, n);
        inPos += n;
        outPos += n;
    }

    /**
     * Copies {@code length} bytes starting {@code distance} bytes back in the output. The source
     * may overlap the bytes being written, so this is done byte by byte.
     *
     * @return false if the reference points before the start of the output
     */
    private boolean copyBack(int distance, int length) {
        int from = outPos - distance;
        if (from < 0) {
            return false;
        }
        int end = Math.min(outPos + length, out.length);
        while (outPos < end) {
            out[outPos++] = out[from++];
        }
        return true;
    }
}
