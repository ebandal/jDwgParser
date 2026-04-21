package io.dwg.core.io;

import java.nio.charset.StandardCharsets;
import io.dwg.core.version.DwgVersion;

/**
 * DWG 스펙 §2의 모든 압축 타입 읽기를 제공하는 핵심 클래스.
 */
public class BitStreamReader {
    private BitInput input;
    private DwgVersion version;

    public BitStreamReader(BitInput input, DwgVersion version) {
        this.input = input;
        this.version = version;
    }

    /**
     * §2.2: BitShort (BS) 읽기
     * 2비트 opcode + 조건부 데이터
     * 비트 레벨 읽기: byte alignment 강제 안 함
     */
    public int readBitShort() {
        int opcode = input.readBits(2);
        switch (opcode) {
            case 0b00: return input.readBits(16) & 0xFFFF;
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
            case 0b11: throw new IllegalStateException("Invalid BL opcode 11");
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
     * §2.4: BitLongLong (BLL) 읽기
     * 가변 길이: 1~3비트 바이트 수 + 데이터
     */
    public long readBitLongLong() {
        int bytesOpcode = input.readBits(3);
        int byteCount;
        if (bytesOpcode == 0) byteCount = 1;
        else if (bytesOpcode == 1) byteCount = 2;
        else if (bytesOpcode == 2) byteCount = 4;
        else if (bytesOpcode == 3) byteCount = 8;
        else byteCount = bytesOpcode - 3;

        long result = 0;
        for (int i = 0; i < byteCount; i++) {
            result |= (long)(input.readBits(8) & 0xFF) << (i * 8);
        }
        return result;
    }

    /**
     * §2.5: BitDouble (BD) 읽기
     * 2비트 opcode + 조건부 데이터
     */
    public double readBitDouble() {
        int opcode = input.readBits(2);
        switch (opcode) {
            case 0b00: return input.readRawDouble();
            case 0b01: return 1.0;
            case 0b10: return 0.0;
            case 0b11: throw new IllegalStateException("Invalid BD opcode 11");
            default: throw new IllegalStateException();
        }
    }

    /**
     * §2.9: BitDouble with Default (BDWMD) 읽기
     */
    public double readBitDoubleWithDefault(double def) {
        int opcode = input.readBits(2);
        switch (opcode) {
            case 0b00: return input.readRawDouble();
            case 0b01: return def;
            case 0b10: return 0.0;
            case 0b11: throw new IllegalStateException("Invalid BDWMD opcode 11");
            default: throw new IllegalStateException();
        }
    }

    /**
     * §2.6: Modular Char (MC) 읽기 - Signed
     * libredwg 호환 구현: high bit=계속 플래그, bit 6=부호 플래그 (최종 바이트만)
     * 비트 레벨 읽기: 바이트 정렬을 강제하지 않음
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
     * UMC (Unsigned Modular Char) 읽기
     * MC 인코딩이지만 부호는 없음 (항상 양수)
     * 비트 레벨 읽기: 바이트 정렬을 강제하지 않음 (libredwg 호환)
     */
    public int readUnsignedModularChar() {
        int result = 0;
        int shift = 0;
        int b;
        boolean debug = false;  // Toggle for debugging
        if (debug) System.out.printf("[DEBUG UMC] Start reading at pos %d\n", input.position());
        do {
            b = input.readBits(8) & 0xFF;  // 비트 레벨: 정렬 강제 없음
            if (debug) System.out.printf("[DEBUG UMC]   byte[%d] = 0x%02X (high=%d, value=0x%02X)\n",
                shift, b, (b & 0x80) >> 7, b & 0x7F);
            result |= (b & 0x7F) << (shift * 7);
            shift++;
        } while ((b & 0x80) != 0);
        if (debug) System.out.printf("[DEBUG UMC] Result = 0x%X (%d)\n", result, result);
        return result;
    }

    /**
     * §2.7: Modular Short (MS) 읽기
     * 비트 레벨 읽기: byte alignment 강제 안 함 (readRawShort() 대신 readBits(16) 사용)
     */
    public int readModularShort() {
        int result = 0;
        int shift = 0;
        int w;
        do {
            w = input.readBits(16) & 0xFFFF;  // Bit-level: no forced alignment
            result |= (w & 0x7FFF) << (shift * 15);
            shift++;
        } while ((w & 0x8000) != 0);

        // 음수 플래그 처리
        if ((w & 0x4000) != 0) {
            result = -result;
        }
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
            int colorType = input.readRawChar();
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
            result = (result << 8) | (input.readRawChar() & 0xFF);
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
            bytes[i] = (byte)input.readRawChar();
        }
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    /**
     * Unicode Text (TU) 읽기
     * BS 문자수 + UTF-16LE
     */
    public String readUnicodeText() {
        int charCount = readBitShort();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < charCount; i++) {
            int low = input.readRawChar() & 0xFF;
            int high = input.readRawChar() & 0xFF;
            sb.append((char)((high << 8) | low));
        }
        return sb.toString();
    }

    /**
     * Variable Text (TV) 읽기
     * R2007+: TU, 미만: T
     */
    public String readVariableText() {
        if (version.usesUnicode()) {
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

    /**
     * 2D Raw Double (2RD) 읽기
     */
    public double[] read2RawDouble() {
        return new double[]{input.readRawDouble(), input.readRawDouble()};
    }

    /**
     * 3D Raw Double (3RD) 읽기
     */
    public double[] read3RawDouble() {
        return new double[]{input.readRawDouble(), input.readRawDouble(), input.readRawDouble()};
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
