package io.dwg.core.util;

/**
 * Decompressor for the LZ77 variant used by R2007 (AC1021) pages, the page map, the section map
 * and the compressed part of the file header.
 *
 * <p>Written from the Open Design Specification for .dwg files, section 5.10 "Decompression".
 *
 * <p>The stream alternates between literal runs, copied from the input, and back references,
 * copied from the output already produced:
 * <ol>
 *   <li>A literal run whose length comes from the first opcode (or, if the first opcode's high
 *       nibble is 2, from the low three bits of the fourth byte).</li>
 *   <li>One or more back references ({@link #readInstruction}). The low three bits of the last
 *       opcode give the length of the next literal run. When they are zero, the next byte is
 *       either another back reference or, if its high nibble is zero, the opcode of a literal
 *       length.</li>
 * </ol>
 *
 * <p>Literal bytes are not copied in order. The specification gives the order as a table for
 * 1–32 bytes ({@link #LITERAL_ORDER}); longer runs are copied in blocks of 32.
 */
public class Lz77Decompressor {

    /**
     * The table of section 5.10.1. Row {@code n} describes how {@code n} literal bytes are copied:
     * a list of blocks {size, source index}, written to the output one after the other. A block of
     * size {@code s} at source index {@code i} is itself copied as row {@code s} applied at
     * {@code i}; a block of size 1 is a single byte. Rows 2, 3, 4, 8, 16 and 32 therefore define
     * the blocks the other rows are made of.
     */
    private static final int[][] LITERAL_ORDER = {
        /*  0 */ {},
        /*  1 */ {1, 0},
        /*  2 */ {1, 1, 1, 0},
        /*  3 */ {1, 2, 1, 1, 1, 0},
        /*  4 */ {1, 0, 1, 1, 1, 2, 1, 3},
        /*  5 */ {1, 4, 4, 0},
        /*  6 */ {1, 5, 4, 1, 1, 0},
        /*  7 */ {2, 5, 4, 1, 1, 0},
        /*  8 */ {4, 0, 4, 4},
        /*  9 */ {1, 8, 8, 0},
        /* 10 */ {1, 9, 8, 1, 1, 0},
        /* 11 */ {2, 9, 8, 1, 1, 0},
        /* 12 */ {4, 8, 8, 0},
        /* 13 */ {1, 12, 4, 8, 8, 0},
        /* 14 */ {1, 13, 4, 9, 8, 1, 1, 0},
        /* 15 */ {2, 13, 4, 9, 8, 1, 1, 0},
        /* 16 */ {8, 8, 8, 0},
        /* 17 */ {8, 9, 1, 8, 8, 0},
        /* 18 */ {1, 17, 16, 1, 1, 0},
        /* 19 */ {3, 16, 16, 0},
        /* 20 */ {4, 16, 16, 0},
        /* 21 */ {1, 20, 4, 16, 16, 0},
        /* 22 */ {2, 20, 4, 16, 16, 0},
        /* 23 */ {3, 20, 4, 16, 16, 0},
        /* 24 */ {8, 16, 16, 0},
        /* 25 */ {8, 17, 1, 16, 16, 0},
        /* 26 */ {1, 25, 8, 17, 1, 16, 16, 0},
        /* 27 */ {2, 25, 8, 17, 1, 16, 16, 0},
        /* 28 */ {4, 24, 8, 16, 16, 0},
        /* 29 */ {1, 28, 4, 24, 8, 16, 16, 0},
        /* 30 */ {2, 28, 4, 24, 8, 16, 16, 0},
        /* 31 */ {1, 30, 4, 26, 8, 18, 16, 2, 2, 0},
        /* 32 */ {16, 16, 16, 0},
    };

    /**
     * {@code PERMUTATION[n][k]} is the input index, relative to the start of the run, of the
     * {@code k}-th output byte of an {@code n}-byte literal run.
     */
    private static final int[][] PERMUTATION = new int[33][];

    static {
        for (int n = 0; n <= 32; n++) {
            int[] p = new int[n];
            int filled = expand(n, 0, p, 0);
            if (filled != n) {
                throw new IllegalStateException("literal order row " + n + " has " + filled + " bytes");
            }
            PERMUTATION[n] = p;
        }
    }

    /** Writes the source indices of row {@code size} applied at {@code base} into {@code p}. */
    private static int expand(int size, int base, int[] p, int at) {
        if (size == 1) {
            p[at] = base;
            return at + 1;
        }
        int[] row = LITERAL_ORDER[size];
        for (int i = 0; i < row.length; i += 2) {
            at = expand(row[i], base + row[i + 1], p, at);
        }
        return at;
    }

    private byte[] in;
    private int inPos;
    private byte[] out;
    private int outPos;

    // Result of readInstruction.
    private int opcode;
    private int distance;
    private int length;

    /**
     * Decompresses {@code compressed} into a buffer of {@code expectedSize} bytes. Decoding stops
     * when the input ends or the buffer is full; the rest of the buffer stays zero.
     *
     * @throws Exception if the data is too short or a back reference points before the start
     */
    public byte[] decompress(byte[] compressed, int expectedSize) throws Exception {
        in = compressed;
        inPos = 0;
        io.dwg.core.io.ReadLimits.reserve((long) (expectedSize), "Lz77Decompressor");
        out = new byte[expectedSize];
        outPos = 0;

        if (in.length < 2) {
            throw new Exception("Compressed data too short");
        }

        opcode = nextByte();
        int literal = 0;
        if ((opcode & 0xF0) == 0x20) {
            inPos += 2;
            if (inPos >= in.length) {
                throw new Exception("Header truncated");
            }
            literal = nextByte() & 0x07;
            if (literal == 0) {
                throw new Exception("Decompression error: zero length");
            }
        }

        while (inPos < in.length) {
            if (literal == 0) {
                literal = literalLength(opcode);
            }
            copyLiterals(literal);
            if (inPos >= in.length) {
                break;
            }

            opcode = nextByte();
            readInstruction();
            while (true) {
                if (outPos + length > out.length) {
                    return out;
                }
                if (distance > outPos) {
                    throw new Exception("Decompression error: offset " + distance + " > dstPos " + outPos);
                }
                copyBack();

                literal = opcode & 0x07;
                if (literal != 0 || inPos >= in.length) {
                    break;
                }
                opcode = nextByte();
                if ((opcode >> 4) == 0) {
                    break; // a literal length follows
                }
                if ((opcode >> 4) == 0x0F) {
                    opcode &= 0x0F;
                }
                readInstruction();
            }
        }
        return out;
    }

    /** Returns the next input byte (0–255), or 0 past the end of the input. */
    private int nextByte() {
        return inPos < in.length ? in[inPos++] & 0xFF : 0;
    }

    /**
     * Literal length from an opcode whose high nibble is zero: opcode + 8. For opcode 0x0F
     * (length 0x17) the next byte is added, and if that byte is 0xFF, 16-bit little-endian
     * values are added until one is not 0xFFFF.
     */
    private int literalLength(int op) {
        int len = op + 8;
        if (len == 0x17) {
            if (inPos >= in.length) {
                return len;
            }
            int n = nextByte();
            len += n;
            if (n == 0xFF) {
                while (inPos + 1 < in.length) {
                    int n2 = nextByte() | (nextByte() << 8);
                    len += n2;
                    if (n2 != 0xFFFF) {
                        break;
                    }
                }
            }
        }
        return len;
    }

    /**
     * Reads one back reference for {@link #opcode}, per the ReadInstructions function of section
     * 5.10.2. Sets {@link #distance}, {@link #length} and the next {@link #opcode}.
     */
    private void readInstruction() {
        int op = opcode;
        switch (op >> 4) {
            case 0: {
                length = (op & 0x0F) + 0x13;
                int low = nextByte();
                op = nextByte();
                length += (op >> 3) & 0x10;
                distance = ((op & 0x78) << 5) + 1 + low;
                break;
            }
            case 1: {
                length = (op & 0x0F) + 3;
                int low = nextByte();
                op = nextByte();
                distance = ((op & 0xF8) << 5) + 1 + low;
                break;
            }
            case 2: {
                int d = nextByte();
                d |= nextByte() << 8;
                length = op & 0x07;
                if ((op & 0x08) == 0) {
                    op = nextByte();
                    length += op & 0xF8;
                } else {
                    d++;
                    length += nextByte() << 3;
                    op = nextByte();
                    length += ((op & 0xF8) << 8) + 0x100;
                }
                distance = d;
                break;
            }
            default: {
                length = op >> 4;
                int low = op & 0x0F;
                op = nextByte();
                distance = ((op & 0xF8) << 1) + low + 1;
                break;
            }
        }
        opcode = op;
    }

    /** Copies a literal run of {@code count} bytes in the order of {@link #LITERAL_ORDER}. */
    private void copyLiterals(int count) {
        count = Math.min(count, Math.min(out.length - outPos, in.length - inPos));
        while (count >= 32) {
            copyBlock(PERMUTATION[32]);
            count -= 32;
        }
        if (count > 0) {
            copyBlock(PERMUTATION[count]);
        }
    }

    private void copyBlock(int[] order) {
        for (int k = 0; k < order.length; k++) {
            out[outPos + k] = in[inPos + order[k]];
        }
        outPos += order.length;
        inPos += order.length;
    }

    /** Copies {@link #length} bytes from {@link #distance} bytes back; the ranges may overlap. */
    private void copyBack() {
        int from = outPos - distance;
        for (int i = 0; i < length; i++) {
            out[outPos + i] = out[from + i];
        }
        outPos += length;
    }
}
