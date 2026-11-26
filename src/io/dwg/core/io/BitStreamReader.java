package io.dwg.core.io;

import io.dwg.core.version.DwgVersion;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Reads the bit codes of chapter 2 of the Open Design Specification for .dwg files from a bit
 * stream.
 *
 * <p>Object and header data are bit streams: a value starts at whatever bit the previous one
 * ended, and multi-byte values are little-endian (least significant byte first) unless noted.
 */
public class BitStreamReader {
    private final BitInput input;
    private final DwgVersion version;
    private Charset textCharset = StandardCharsets.US_ASCII;
    private Object context;
    private boolean stringStreamMode;
    private BitStreamReader stringStream;
    private long emptyStringBudget = Long.MAX_VALUE;

    public BitStreamReader(BitInput input, DwgVersion version) {
        this.input = input;
        this.version = version;
    }

    // ---------------------------------------------------------------------------------------
    // state
    // ---------------------------------------------------------------------------------------

    /**
     * Sets the string stream of an R2007+ object. Afterwards {@link #readVariableText()} reads
     * from it instead of from this stream.
     *
     * @param stringInput input positioned at the start of the string stream, or null if the
     *                    object has no strings
     */
    public void setStringStream(BitInput stringInput) {
        this.stringStreamMode = true;
        this.stringStream = stringInput == null ? null : new BitStreamReader(stringInput, version);
        // an object without a string stream has only empty strings; more of them than the object
        // has bits cannot be real data (a corrupt count would otherwise loop without reading)
        this.emptyStringBudget = bitsLeft();
    }

    /**
     * The bits left in the input: up to its limit (the end of the object being read) or its
     * end; {@link Long#MAX_VALUE} for inputs that do not tell.
     */
    public long bitsLeft() {
        return input instanceof ByteBufferBitInput ? ((ByteBufferBitInput) input).bitsLeft() : Long.MAX_VALUE;
    }

    /**
     * A count or size read from the data, checked against what is left to read: {@code n} items
     * of at least {@code minBitsEach} bits each must fit in {@link #bitsLeft()}. A corrupt file
     * then fails here instead of making the reader allocate or loop without end.
     *
     * @throws IllegalStateException if {@code n} is negative or too large
     */
    public int count(long n, int minBitsEach) {
        if (n < 0 || n > Integer.MAX_VALUE || n * Math.max(1, minBitsEach) > bitsLeft()) {
            throw new IllegalStateException("count " + n + " does not fit in the " + bitsLeft() + " bits left");
        }
        return (int) n;
    }

    /** State that the caller keeps while it reads one object. */
    public Object context() {
        return context;
    }

    public void setContext(Object context) {
        this.context = context;
    }

    /** Character set for T strings (R2004 and earlier), from the drawing's code page. */
    public void setTextCharset(Charset textCharset) {
        if (textCharset != null) {
            this.textCharset = textCharset;
        }
    }

    /** The character set T strings are decoded with. */
    public Charset textCharset() {
        return textCharset;
    }

    public long position() {
        return input.position();
    }

    public void seek(long bitPos) {
        input.seek(bitPos);
    }

    public boolean isEof() {
        return input.isEof();
    }

    public BitInput getInput() {
        return input;
    }

    // ---------------------------------------------------------------------------------------
    // building blocks
    // ---------------------------------------------------------------------------------------

    private int nextByte() {
        return input.readBits(8) & 0xFF;
    }

    /** {@code count} bytes as a little-endian number. */
    private long littleEndian(int count) {
        long value = 0;
        for (int i = 0; i < count; i++) {
            value |= (long) nextByte() << (8 * i);
        }
        return value;
    }

    private int twoBitCode() {
        return input.readBits(2);
    }

    // ---------------------------------------------------------------------------------------
    // integers
    // ---------------------------------------------------------------------------------------

    /** RC (§2): a raw byte, unsigned. */
    public int readRawChar() {
        return nextByte();
    }

    /** RS (§2): a raw short, least significant byte first, unsigned. */
    public int readRawShort() {
        return (int) littleEndian(2);
    }

    /** BITSHORT (§2.2): 00 a short follows, 01 an unsigned byte follows, 10 is 0, 11 is 256. */
    public int readBitShort() {
        switch (twoBitCode()) {
            case 0:  return (int) littleEndian(2);
            case 1:  return nextByte();
            case 2:  return 0;
            default: return 256;
        }
    }

    /**
     * BITLONG (§2.3): 00 a long follows, 01 an unsigned byte follows, 10 is 0. Code 11 is
     * not used by the format; it is read as 256, like a BITSHORT.
     */
    public int readBitLong() {
        switch (twoBitCode()) {
            case 0:  return (int) littleEndian(4);
            case 1:  return nextByte();
            case 2:  return 0;
            default: return 256;
        }
    }

    /**
     * BITLONGLONG (§2.4): a byte count, then that many bytes.
     *
     * <p>The specification says the count is a 3B (§2.1), but a 3B can only take the values
     * 0, 2, 6 and 7, which cannot describe 1, 3, 4 or 5 bytes. The count is therefore read as a
     * plain 3-bit number (0–7). Not yet checked against a file: none of the samples has a value
     * read with this method.
     */
    public long readBitLongLong() {
        return littleEndian(input.readBits(3));
    }

    /**
     * Object type of R2010+ (§2.12): a bit pair, then 0: one byte; 1: one byte + 0x1F0;
     * 2 (and 3): a raw short.
     */
    public int readBOT() {
        int code = twoBitCode();
        if (code == 0) {
            return nextByte();
        }
        if (code == 1) {
            return nextByte() + 0x1F0;
        }
        return (int) littleEndian(2);
    }

    /**
     * Modular char (§2.6): bytes of 7 value bits, least significant first; the high bit is set
     * while another byte follows. In the last byte, bit 0x40 is a sign flag and not a value bit.
     */
    public int readModularChar() {
        int value = 0;
        for (int shift = 0; ; shift += 7) {
            int b = nextByte();
            if ((b & 0x80) != 0) {
                value |= (b & 0x7F) << shift;
                continue;
            }
            value |= (b & 0x3F) << shift;
            return (b & 0x40) != 0 ? -value : value;
        }
    }

    /**
     * Modular char without the sign flag, as used for the handle offsets of the object map
     * (§2.6, last paragraph).
     */
    public int readUnsignedModularChar() {
        int value = 0;
        int b;
        int shift = 0;
        do {
            b = nextByte();
            value |= (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);
        return value;
    }

    /**
     * Unsigned modular char limited to 8 bytes; used for the handle stream size of R2010+
     * objects.
     */
    public long readUMC() {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            int b = nextByte();
            value |= (long) (b & 0x7F) << (7 * i);
            if ((b & 0x80) == 0) {
                break;
            }
        }
        return value;
    }

    /**
     * Modular short (§2.7): little-endian shorts of 15 value bits, least significant first; the
     * high bit is set while another short follows. Never negative.
     */
    public int readModularShort() {
        int value = 0;
        int shift = 0;
        int word;
        do {
            word = (int) littleEndian(2);
            value |= (word & 0x7FFF) << shift;
            shift += 15;
        } while ((word & 0x8000) != 0);
        return value;
    }

    /** A raw short stored most significant byte first, as in the object map (§23). */
    public int readBigEndianShort() {
        int high = nextByte();
        return (high << 8) | nextByte();
    }

    // ---------------------------------------------------------------------------------------
    // doubles
    // ---------------------------------------------------------------------------------------

    /** RD: an IEEE 754 double, 8 bytes. */
    public double readRawDouble() {
        return Double.longBitsToDouble(littleEndian(8));
    }

    public double[] read2RawDouble() {
        return new double[] {readRawDouble(), readRawDouble()};
    }

    public double[] read3RawDouble() {
        return new double[] {readRawDouble(), readRawDouble(), readRawDouble()};
    }

    /** BITDOUBLE (§2.5): 00 a raw double follows, 01 is 1.0, 10 is 0.0, 11 is not used. */
    public double readBitDouble() {
        switch (twoBitCode()) {
            case 0:  return readRawDouble();
            case 1:  return 1.0;
            case 2:  return 0.0;
            default: throw new IllegalStateException("Invalid BD opcode 11");
        }
    }

    public double[] read2BitDouble() {
        return new double[] {readBitDouble(), readBitDouble()};
    }

    public double[] read3BitDouble() {
        return new double[] {readBitDouble(), readBitDouble(), readBitDouble()};
    }

    /**
     * BitDouble With Default, DD (§2.9). The bytes of the default are numbered 0–7
     * little-endian:
     *   00 = the default;
     *   01 = 4 bytes follow and replace bytes 0–3;
     *   10 = 6 bytes follow: the first 2 replace bytes 4–5, the last 4 replace bytes 0–3;
     *   11 = a raw double follows.
     */
    public double readDD(double defaultValue) {
        long bits = Double.doubleToRawLongBits(defaultValue);
        switch (twoBitCode()) {
            case 0:
                return defaultValue;
            case 1:
                bits = (bits & 0xFFFFFFFF00000000L) | littleEndian(4);
                return Double.longBitsToDouble(bits);
            case 2: {
                long bytes45 = littleEndian(2) << 32;
                long bytes03 = littleEndian(4);
                bits = (bits & 0xFFFF000000000000L) | bytes45 | bytes03;
                return Double.longBitsToDouble(bits);
            }
            default:
                return readRawDouble();
        }
    }

    public double[] read2DD(double defaultX, double defaultY) {
        return new double[] {readDD(defaultX), readDD(defaultY)};
    }

    public double[] read3DD(double defaultX, double defaultY, double defaultZ) {
        return new double[] {readDD(defaultX), readDD(defaultY), readDD(defaultZ)};
    }

    /**
     * Bit Extrusion (§2.8): R13–R14 a 3BD; R2000+ one bit, which if set means (0, 0, 1),
     * otherwise a 3BD follows.
     */
    public double[] readBitExtrusion() {
        if (version.from(DwgVersion.R2000) && input.readBit()) {
            return new double[] {0.0, 0.0, 1.0};
        }
        return read3BitDouble();
    }

    /**
     * BitThickness (§2.10): R13–R14 a BD; R2000+ one bit, which if set means 0.0, otherwise a
     * BD follows.
     */
    public double readBitThickness() {
        if (version.from(DwgVersion.R2000) && input.readBit()) {
            return 0.0;
        }
        return readBitDouble();
    }

    // ---------------------------------------------------------------------------------------
    // colors and handles
    // ---------------------------------------------------------------------------------------

    /**
     * CmColor (§2.11). R15 and earlier: a BS color index. R2004+: BS index, BL RGB value and an
     * RC whose bit 1 means a color name (TV) follows and bit 2 a book name (TV).
     *
     * @return {index} or {index, rgb, flags}
     */
    public int[] readCmColor() {
        if (!version.from(DwgVersion.R2004)) {
            return new int[] {readBitShort()};
        }
        return readTrueColor();
    }

    /**
     * TC (§2): a color in the R2004+ CMC form, in every version (the table style data uses it in
     * older files too).
     *
     * @return {index, rgb, flags}
     */
    public int[] readTrueColor() {
        int index = readBitShort();
        int rgb = readBitLong();
        int flags = nextByte();
        if ((flags & 1) != 0) {
            readVariableText();
        }
        if ((flags & 2) != 0) {
            readVariableText();
        }
        return new int[] {index, rgb, flags};
    }

    /**
     * Reads a handle reference (§2.13) and returns its code and value packed together: the
     * value in the low bytes and the code in the byte above them.
     */
    public long readHandle() {
        int code = input.readBits(4);
        int length = input.readBits(4);
        long value = bigEndian(length);
        return (((long) code << 8) << ((length - 1) * 8)) | value;
    }

    /**
     * Reads a handle reference (§2.13) and makes it absolute. Codes 6 and 8 mean the handle one
     * after / before the containing object's; 0xA and 0xC add / subtract the value; the other
     * codes hold the handle itself.
     *
     * @param containingHandle the handle of the object that holds the reference
     */
    public long readHandleReference(long containingHandle) {
        int code = input.readBits(4);
        long value = bigEndian(input.readBits(4));
        switch (code) {
            case 0x6: return containingHandle + 1;
            case 0x8: return containingHandle - 1;
            case 0xA: return containingHandle + value;
            case 0xC: return containingHandle - value;
            default:  return value;
        }
    }

    /**
     * {@link #readHandleReference} as a {@link io.dwg.core.type.DwgHandleRef}; references to
     * the same handle made recently are one instance ({@link io.dwg.core.type.DwgHandleRef#of}).
     */
    public io.dwg.core.type.DwgHandleRef readRef(long containingHandle) {
        return io.dwg.core.type.DwgHandleRef.of(readHandleReference(containingHandle));
    }

    /**
     * Older form of {@link #readHandleReference}: interprets the packed value of
     * {@link #readHandle()}.
     */
    public long readHandleRef(long ownerHandle) {
        long packed = readHandle();
        long low = packed & 0xFFFFFFFFL;
        switch ((int) (packed >> 56) & 0x0F) {
            case 0:  return packed & 0xFFFFFFFFFFFFFFFL;
            case 1:  return ownerHandle + low;
            case 2:  return ownerHandle - low;
            case 4:
            case 8:  return low;
            default: return packed;
        }
    }

    /** Handle bytes are stored most significant first. */
    private long bigEndian(int count) {
        long value = 0;
        for (int i = 0; i < count; i++) {
            value = (value << 8) | nextByte();
        }
        return value;
    }

    // ---------------------------------------------------------------------------------------
    // text
    // ---------------------------------------------------------------------------------------

    /** T: a BS byte count, then the bytes in the drawing's code page. Trailing NULs are dropped. */
    public String readText() {
        int count = readBitShort();
        byte[] bytes = new byte[count];
        for (int i = 0; i < count; i++) {
            bytes[i] = (byte) nextByte();
        }
        int end = count;
        while (end > 0 && bytes[end - 1] == 0) {
            end--;
        }
        return new String(bytes, 0, end, textCharset);
    }

    /** TU: a BS character count, then UTF-16LE characters. Trailing NULs are dropped. */
    public String readUnicodeText() {
        int count = readBitShort();
        StringBuilder text = new StringBuilder(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            text.append((char) littleEndian(2));
        }
        int end = text.length();
        while (end > 0 && text.charAt(end - 1) == 0) {
            end--;
        }
        text.setLength(end);
        return text.toString();
    }

    /**
     * TV: T up to R2004, TU from R2007. For R2007+ objects whose string stream was set, the
     * strings come from that stream, in order; without a stream the object has no strings.
     */
    public String readVariableText() {
        if (!version.usesUnicode()) {
            return readText();
        }
        if (stringStreamMode) {
            if (stringStream == null) {
                if (--emptyStringBudget < 0) {
                    throw new IllegalStateException("more strings than an object without a string stream can hold");
                }
                return "";
            }
            return stringStream.readUnicodeText();
        }
        return readUnicodeText();
    }
}
