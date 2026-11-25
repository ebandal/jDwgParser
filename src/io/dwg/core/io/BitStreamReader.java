package io.dwg.core.io;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import io.dwg.core.version.DwgVersion;

/**
 * DWG 스펙 §2의 모든 압축 타입 읽기를 제공하는 핵심 클래스.
 */
public class BitStreamReader {
    private BitInput input;
    private DwgVersion version;
    private Charset textCharset = StandardCharsets.US_ASCII;
    private Object context;
    private boolean stringStreamMode;
    private BitStreamReader stringStream;

    public BitStreamReader(BitInput input, DwgVersion version) {
        this.input = input;
        this.version = version;
    }

    /**
     * R2007+ 객체의 문자열 스트림을 지정한다. 지정한 뒤에는 {@link #readVariableText()}가
     * 본 스트림이 아니라 문자열 스트림에서 읽는다.
     *
     * @param stringInput 문자열 스트림의 시작에 놓인 입력. 객체에 문자열이 없으면 null
     */
    public void setStringStream(BitInput stringInput) {
        this.stringStreamMode = true;
        this.stringStream = stringInput == null ? null : new BitStreamReader(stringInput, version);
    }

    /**
     * 이 리더로 읽는 대상에 딸린 상태. 상위 계층이 객체 하나를 읽는 동안의 값을 보관하는 데 쓴다.
     */
    public Object context() {
        return context;
    }

    public void setContext(Object context) {
        this.context = context;
    }

    /**
     * 핸들 참조(H)를 읽어 절대 핸들로 바꾼다.
     * 코드 6, 8, 0xA, 0xC는 참조를 담고 있는 객체의 핸들에 대한 상대값이고, 나머지는 절대값이다.
     *
     * @param containingHandle 이 참조를 담고 있는 객체의 핸들
     */
    public long readHandleReference(long containingHandle) {
        int code = input.readBits(4);
        int counter = input.readBits(4);
        long value = 0;
        for (int i = 0; i < counter; i++) {
            value = (value << 8) | (input.readBits(8) & 0xFF);
        }
        switch (code) {
            case 0x6: return containingHandle + 1;
            case 0x8: return containingHandle - 1;
            case 0xA: return containingHandle + value;
            case 0xC: return containingHandle - value;
            default:  return value;
        }
    }

    /**
     * T 문자열(R2004 이하)을 디코딩할 문자 집합. 도면의 코드페이지에 맞춰 지정한다.
     */
    public void setTextCharset(Charset textCharset) {
        if (textCharset != null) {
            this.textCharset = textCharset;
        }
    }

    /**
     * §2.2: BitShort (BS) 읽기
     * 2비트 opcode + 조건부 데이터
     * 비트 레벨 읽기: byte alignment 강제 안 함
     */
    public int readBitShort() {
        int opcode = input.readBits(2);
        switch (opcode) {
            case 0b00: {
                int lo = input.readBits(8) & 0xFF;
                int hi = input.readBits(8) & 0xFF;
                return lo | (hi << 8);
            }
            case 0b01: return input.readBits(8) & 0xFF;
            case 0b10: return 0;
            case 0b11: return 256;
            default: throw new IllegalStateException();
        }
    }

    /**
     * §2.3: BitLong (BL) 읽기
     * 2비트 opcode + 조건부 데이터
     * 비트 레벨 읽기: byte alignment 강제 안 함
     */
    public int readBitLong() {
        int opcode = input.readBits(2);
        switch (opcode) {
            case 0b00: return readBits32();  // Read 32 bits
            case 0b01: return input.readBits(8) & 0xFF;
            case 0b10: return 0;
            case 0b11: return 256;
            default: throw new IllegalStateException();
        }
    }

    private int readBits32() {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result |= (input.readBits(8) & 0xFF) << (i * 8);
        }
        return result;
    }

    /**
     * Unsigned modular char (spec §2.6, without the negation): bytes, least significant first,
     * each holding 7 value bits, with the high bit set while another byte follows. Reads at most
     * 8 bytes. Used for the handle stream size of R2010+ objects.
     */
    public long readUMC() {
        long result = 0;
        int shift = 0;
        for (int i = 0; i < 8; i++) {
            int b = input.readBits(8) & 0xFF;
            result |= (long)(b & 0x7F) << shift;
            shift += 7;
            if ((b & 0x80) == 0) break;
        }
        return result;
    }

    /**
     * Object type of R2010+ (spec §2.12): a bit pair, then
     * 0: one byte; 1: one byte + 0x1F0; 2 (and 3): a raw short, little-endian.
     */
    public int readBOT() {
        int bb = input.readBits(2);
        if (bb == 0) return input.readBits(8) & 0xFF;
        else if (bb == 1) return (input.readBits(8) & 0xFF) + 0x1F0;
        else {
            int lo = input.readBits(8) & 0xFF;
            int hi = input.readBits(8) & 0xFF;
            return lo | (hi << 8);
        }
    }

    /**
     * BitLongLong (spec §2.4): a byte count, then that many bytes, least significant first.
     *
     * <p>The specification says the count is a 3B (§2.1), but a 3B can only take the values
     * 0, 2, 6 and 7, which cannot describe 1, 3, 4 or 5 bytes. The count is therefore read as a
     * plain 3-bit number (0–7). Not yet checked against a file: none of the samples has a value
     * read with this method (see TODO.md).
     */
    public long readBitLongLong() {
        int len = input.readBits(3);
        long result = 0;
        for (int i = 0; i < len; i++) {
            result |= (long)(input.readBits(8) & 0xFF) << (i * 8);
        }
        return result;
    }

    /**
     * BitDouble (spec §2.5): 2-bit code; 00: a raw double (8 bytes, not byte aligned),
     * 01: 1.0, 10: 0.0, 11: not used.
     */
    public double readBitDouble() {
        int opcode = input.readBits(2);
        switch (opcode) {
            case 0b00: return readRawDoubleNoAlign();
            case 0b01: return 1.0;
            case 0b10: return 0.0;
            case 0b11: throw new IllegalStateException("Invalid BD opcode 11");
            default: throw new IllegalStateException();
        }
    }

    /**
     * BitDouble With Default, DD (spec §2.9). 2-bit code, with the bytes of the default value
     * taken little-endian:
     *   00 = the default;
     *   01 = 4 bytes follow and replace bytes 0–3 of the default;
     *   10 = 6 bytes follow: the first 2 replace bytes 4–5, the last 4 replace bytes 0–3;
     *   11 = a raw double follows.
     */
    public double readDD(double def) {
        int opcode = input.readBits(2);
        switch (opcode) {
            case 0b00: return def;
            case 0b11: return readRawDoubleNoAlign();
            case 0b01: {
                // read bytes [0,1,2,3] from stream; keep default bytes [4,5,6,7]
                long defBits = Double.doubleToRawLongBits(def);
                long lo = (long)(input.readBits(8) & 0xFF)
                        | (long)(input.readBits(8) & 0xFF) << 8
                        | (long)(input.readBits(8) & 0xFF) << 16
                        | (long)(input.readBits(8) & 0xFF) << 24;
                return Double.longBitsToDouble(lo | (defBits & 0xFFFFFFFF00000000L));
            }
            case 0b10: {
                // read bytes in stream order [4,5,0,1,2,3]; keep default bytes [6,7]
                long defBits = Double.doubleToRawLongBits(def);
                long b4 = (long)(input.readBits(8) & 0xFF) << 32;
                long b5 = (long)(input.readBits(8) & 0xFF) << 40;
                long lo = (long)(input.readBits(8) & 0xFF)
                        | (long)(input.readBits(8) & 0xFF) << 8
                        | (long)(input.readBits(8) & 0xFF) << 16
                        | (long)(input.readBits(8) & 0xFF) << 24;
                return Double.longBitsToDouble(b4 | b5 | lo | (defBits & 0xFFFF000000000000L));
            }
            default: throw new IllegalStateException();
        }
    }

    /** 2DD: x와 y를 각각 DD로 읽기 */
    public double[] read2DD(double defX, double defY) {
        return new double[]{readDD(defX), readDD(defY)};
    }

    /** 3DD: x, y, z를 각각 DD로 읽기 */
    public double[] read3DD(double defX, double defY, double defZ) {
        return new double[]{readDD(defX), readDD(defY), readDD(defZ)};
    }

    /**
     * Raw double: 8 bytes of an IEEE 754 double, least significant first, read at the current
     * bit position (bit streams are not byte aligned).
     */
    private double readRawDoubleNoAlign() {
        long bits = 0;
        for (int i = 0; i < 8; i++) {
            bits |= (long)(input.readBits(8) & 0xFF) << (i * 8);
        }
        return Double.longBitsToDouble(bits);
    }

    /**
     * Modular char (spec §2.6): bytes, least significant first, 7 value bits each, the high bit
     * set while another byte follows. In the last byte, bit 0x40 means the number is negative and
     * is not part of the value. Read at the current bit position.
     */
    public int readModularChar() {
        int result = 0;
        int shift = 0;
        int b;
        boolean negative = false;

        do {
            b = input.readBits(8) & 0xFF;

            if ((b & 0x80) == 0) {
                // 최종 바이트 (high bit = 0)
                if ((b & 0x40) != 0) {
                    // Bit 6이 부호 플래그
                    negative = true;
                    b &= 0xBF;  // 비트 6 제거
                }
                // 최종 바이트는 6비트 데이터만 포함
                result |= (b & 0x3F) << (shift * 7);
                break;
            } else {
                // 계속되는 바이트 (high bit = 1): 7비트 데이터
                result |= (b & 0x7F) << (shift * 7);
                shift++;
            }
        } while (true);

        return negative ? -result : result;
    }

    /**
     * Modular char without negation, as used for handle offsets in the object map (spec §2.6,
     * last paragraph). Read at the current bit position.
     */
    public int readUnsignedModularChar() {
        int result = 0;
        int shift = 0;
        int b;
        do {
            b = input.readBits(8) & 0xFF;
            result |= (b & 0x7F) << (shift * 7);
            shift++;
        } while ((b & 0x80) != 0);
        return result;
    }

    /**
     * Modular short (spec §2.7): like a modular char, but in units of little-endian shorts with
     * 15 value bits each and the high bit set while another short follows. Never negative.
     */
    public int readModularShort() {
        int result = 0;
        int shift = 0;
        int w;
        do {
            // little-endian: low byte first
            int lo = input.readBits(8) & 0xFF;
            int hi = input.readBits(8) & 0xFF;
            w = lo | (hi << 8);
            result |= (w & 0x7FFF) << (shift * 15);
            shift++;
        } while ((w & 0x8000) != 0);
        return result;
    }

    /**
     * RS_BE (Big-Endian) 읽기 - Handles 섹션용
     * Handles 섹션의 block_size와 CRC는 big-endian (RS_BE)로 저장됨.
     * 모든 DWG 버전(R13~R2018)에서 동일.
     * 비트 레벨 읽기: MC 이후 bit 경계에서 읽을 수 있어야 함
     */
    public int readBigEndianShort() {
        int byte1 = input.readBits(8) & 0xFF;
        int byte2 = input.readBits(8) & 0xFF;
        return (byte1 << 8) | byte2;  // MSB first (big-endian)
    }

    /**
     * §2.8: BitExtrusion (BE) 읽기
     * R13-14: 3BD, R2000+: single bit → 0,0,1 or 3BD
     */
    public double[] readBitExtrusion() {
        if (version.until(DwgVersion.R14)) {
            return read3BitDouble();
        } else {
            boolean flag = input.readBit();
            if (flag) {
                return new double[]{0.0, 0.0, 1.0};
            } else {
                return read3BitDouble();
            }
        }
    }

    /**
     * §2.10: BitThickness (BT) 읽기
     * R13-14: BD, R2000+: bit → 0.0 or BD
     */
    public double readBitThickness() {
        if (version.until(DwgVersion.R14)) {
            return readBitDouble();
        } else {
            boolean flag = input.readBit();
            if (flag) {
                return 0.0;
            } else {
                return readBitDouble();
            }
        }
    }

    /**
     * §2.11: CMColor 읽기
     * R15 이하: BS, R2004+: BS + BL + RC
     */
    public int[] readCmColor() {
        int colorIndex = readBitShort();
        if (version.from(DwgVersion.R2004)) {
            int rgb = readBitLong();
            // RC는 비트 스트림 안에서 바이트 경계에 맞추지 않는다
            int colorType = input.readBits(8) & 0xFF;
            if ((colorType & 1) != 0) readVariableText();   // color name
            if ((colorType & 2) != 0) readVariableText();   // book name
            return new int[]{colorIndex, rgb, colorType};
        }
        return new int[]{colorIndex};
    }

    /**
     * Handle 읽기
     */
    public long readHandle() {
        int code = input.readBits(4);
        int counter = input.readBits(4);

        long result = 0;
        for (int i = 0; i < counter; i++) {
            result = (result << 8) | (input.readBits(8) & 0xFF);
        }

        return (((long)code << 8) << ((counter - 1) * 8)) | result;
    }

    /**
     * Handle 참조 해석
     */
    public long readHandleRef(long ownerHandle) {
        long rawHandle = readHandle();
        int code = (int)(rawHandle >> 56) & 0x0F;
        
        switch (code) {
            case 0: return rawHandle & 0xFFFFFFFFFFFFFFFL;  // absolute
            case 1: return ownerHandle + (rawHandle & 0xFFFFFFFFL);  // relative to owner
            case 2: return ownerHandle - (rawHandle & 0xFFFFFFFFL);  // relative negative
            case 4: return (rawHandle & 0xFFFFFFFFL);  // previous owned
            case 8: return (rawHandle & 0xFFFFFFFFL);  // next owned
            default: return rawHandle;
        }
    }

    /**
     * Text (T) 읽기
     * BS길이 + RC 배열
     */
    public String readText() {
        int length = readBitShort();
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte)(input.readBits(8) & 0xFF);
        }
        // 문자열 끝의 NUL은 값에 포함하지 않는다
        while (length > 0 && bytes[length - 1] == 0) {
            length--;
        }
        return new String(bytes, 0, length, textCharset);
    }

    /**
     * Unicode Text (TU) 읽기
     * BS 문자수 + UTF-16LE
     */
    public String readUnicodeText() {
        int charCount = readBitShort();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < charCount; i++) {
            int low = input.readBits(8) & 0xFF;
            int high = input.readBits(8) & 0xFF;
            sb.append((char)((high << 8) | low));
        }
        // 문자열 끝의 NUL은 값에 포함하지 않는다
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * Variable Text (TV) 읽기
     * R2007+: TU, 미만: T
     */
    public String readVariableText() {
        if (version.usesUnicode()) {
            if (stringStreamMode) {
                // 객체의 문자열은 문자열 스트림에 순서대로 들어 있다. 스트림이 없으면 문자열도 없다.
                return stringStream == null ? "" : stringStream.readUnicodeText();
            }
            return readUnicodeText();
        } else {
            return readText();
        }
    }

    /**
     * 2D Bit Double (2BD) 읽기
     */
    public double[] read2BitDouble() {
        return new double[]{readBitDouble(), readBitDouble()};
    }

    /**
     * 3D Bit Double (3BD) 읽기
     */
    public double[] read3BitDouble() {
        return new double[]{readBitDouble(), readBitDouble(), readBitDouble()};
    }

    /** Raw double (RD): 8 bytes, least significant first, at the current bit position. */
    public double readRawDouble() {
        return readRawDoubleNoAlign();
    }

    /**
     * 2D Raw Double (2RD) 읽기
     */
    public double[] read2RawDouble() {
        return new double[]{readRawDoubleNoAlign(), readRawDoubleNoAlign()};
    }

    /**
     * 3D Raw Double (3RD) 읽기
     */
    public double[] read3RawDouble() {
        return new double[]{readRawDoubleNoAlign(), readRawDoubleNoAlign(), readRawDoubleNoAlign()};
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
}
