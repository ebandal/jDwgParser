package io.dwg.core.io;

import java.nio.charset.StandardCharsets;
import io.dwg.core.version.DwgVersion;

/**
 * DWG 스펙 §2의 모든 압축 타입 쓰기를 제공하는 핵심 클래스.
 */
public class BitStreamWriter {
    private BitOutput output;
    private DwgVersion version;

    public BitStreamWriter(BitOutput output, DwgVersion version) {
        this.output = output;
        this.version = version;
    }

    /**
     * §2.2: BitShort (BS) 쓰기
     */
    public void writeBitShort(int value) {
        if (value == 0) {
            output.writeBits(0b10, 2);
        } else if (value == 256) {
            output.writeBits(0b11, 2);
        } else if (value >= 0 && value <= 255) {
            output.writeBits(0b01, 2);
            output.writeRawChar(value);
        } else if (value >= 0 && value <= 65535) {
            output.writeBits(0b00, 2);
            output.writeRawShort((short)value);
        } else {
            throw new IllegalArgumentException("Value out of range for BitShort: " + value);
        }
    }

    /**
     * §2.3: BitLong (BL) 쓰기
     */
    public void writeBitLong(int value) {
        if (value == 0) {
            output.writeBits(0b10, 2);
        } else if (value >= 0 && value <= 255) {
            output.writeBits(0b01, 2);
            output.writeRawChar(value);
        } else {
            output.writeBits(0b00, 2);
            output.writeRawLong(value);
        }
    }

    /**
     * §2.4: BitLongLong (BLL) 쓰기
     */
    public void writeBitLongLong(long value) {
        int byteCount;
        if (value >= 0 && value < 256) {
            byteCount = 1;
        } else if (value >= 0 && value < 65536) {
            byteCount = 2;
        } else if (value >= 0 && value < (1L << 32)) {
            byteCount = 4;
        } else {
            byteCount = 8;
        }

        int opcode;
        if (byteCount == 1) opcode = 0;
        else if (byteCount == 2) opcode = 1;
        else if (byteCount == 4) opcode = 2;
        else opcode = 3;

        output.writeBits(opcode, 3);
        for (int i = 0; i < byteCount; i++) {
            output.writeRawChar((int)((value >> (i * 8)) & 0xFF));
        }
    }

    /**
     * §2.5: BitDouble (BD) 쓰기
     */
    public void writeBitDouble(double value) {
        if (value == 0.0) {
            output.writeBits(0b10, 2);
        } else if (value == 1.0) {
            output.writeBits(0b01, 2);
        } else {
            output.writeBits(0b00, 2);
            output.writeRawDouble(value);
        }
    }

    /**
     * §2.9: BitDouble with Default (BDWMD) 쓰기
     */
    public void writeBitDoubleWithDefault(double value, double def) {
        if (value == 0.0) {
            output.writeBits(0b10, 2);
        } else if (value == def) {
            output.writeBits(0b01, 2);
        } else {
            output.writeBits(0b00, 2);
            output.writeRawDouble(value);
        }
    }

    /**
     * §2.6: Modular Char (MC) 쓰기
     */
    public void writeModularChar(int value) {
        boolean negative = value < 0;
        int absValue = Math.abs(value);

        int[] bytes = new int[5];
        int count = 0;
        int remaining = absValue;

        do {
            int b = remaining & 0x7F;
            remaining >>= 7;
            if (remaining > 0) {
                b |= 0x80;
            }
            if (negative && count == 0) {
                b |= 0x40;
            }
            bytes[count++] = b;
        } while (remaining > 0);

        for (int i = 0; i < count; i++) {
            output.writeRawChar(bytes[i]);
        }
    }

    /**
     * §2.7: Modular Short (MS) 쓰기
     */
    public void writeModularShort(int value) {
        boolean negative = value < 0;
        int absValue = Math.abs(value);

        int[] words = new int[5];
        int count = 0;
        int remaining = absValue;

        do {
            int w = remaining & 0x7FFF;
            remaining >>= 15;
            if (remaining > 0) {
                w |= 0x8000;
            }
            if (negative && count == 0) {
                w |= 0x4000;
            }
            words[count++] = w;
        } while (remaining > 0);

        for (int i = 0; i < count; i++) {
            output.writeRawShort((short)words[i]);
        }
    }

    /**
     * §2.8: BitExtrusion (BE) 쓰기
     */
    public void writeBitExtrusion(double[] vec) {
        if (version.until(DwgVersion.R14)) {
            write3BitDouble(vec);
        } else {
            if (vec.length >= 3 && vec[0] == 0.0 && vec[1] == 0.0 && vec[2] == 1.0) {
                output.writeBit(true);
            } else {
                output.writeBit(false);
                write3BitDouble(vec);
            }
        }
    }

    /**
     * §2.10: BitThickness (BT) 쓰기
     */
    public void writeBitThickness(double value) {
        if (version.until(DwgVersion.R14)) {
            writeBitDouble(value);
        } else {
            if (value == 0.0) {
                output.writeBit(true);
            } else {
                output.writeBit(false);
                writeBitDouble(value);
            }
        }
    }

    /**
     * §2.11: CMColor 쓰기
     */
    public void writeCmColor(int[] color) {
        writeBitShort(color[0]);
        if (version.from(DwgVersion.R2004)) {
            writeBitLong(color.length > 1 ? color[1] : 0);
            output.writeRawChar(color.length > 2 ? color[2] : 0);
        }
    }

    /**
     * Handle 쓰기
     */
    public void writeHandle(long handle) {
        int code = (int)((handle >> 56) & 0x0F);
        int counter = 1;
        
        long tempHandle = handle & 0xFFFFFFFFFFFFFFFL;
        while ((tempHandle >> (counter * 8)) != 0 && counter < 8) {
            counter++;
        }

        output.writeBits(code, 4);
        output.writeBits(counter, 4);

        for (int i = 0; i < counter; i++) {
            output.writeRawChar((int)((handle >> (i * 8)) & 0xFF));
        }
    }

    /**
     * Text (T) 쓰기
     */
    public void writeText(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
        writeBitShort(bytes.length);
        for (byte b : bytes) {
            output.writeRawChar(b & 0xFF);
        }
    }

    /**
     * Unicode Text (TU) 쓰기
     */
    public void writeUnicodeText(String text) {
        writeBitShort(text.length());
        for (char c : text.toCharArray()) {
            output.writeRawChar(c & 0xFF);
            output.writeRawChar((c >> 8) & 0xFF);
        }
    }

    /**
     * Variable Text (TV) 쓰기
     */
    public void writeVariableText(String text) {
        if (version.usesUnicode()) {
            writeUnicodeText(text);
        } else {
            writeText(text);
        }
    }

    /**
     * 2D Bit Double (2BD) 쓰기
     */
    public void write2BitDouble(double[] values) {
        if (values.length < 2) {
            throw new IllegalArgumentException("Expected at least 2 values");
        }
        writeBitDouble(values[0]);
        writeBitDouble(values[1]);
    }

    /**
     * 3D Bit Double (3BD) 쓰기
     */
    public void write3BitDouble(double[] values) {
        if (values.length < 3) {
            throw new IllegalArgumentException("Expected at least 3 values");
        }
        writeBitDouble(values[0]);
        writeBitDouble(values[1]);
        writeBitDouble(values[2]);
    }

    /**
     * 2D Raw Double (2RD) 쓰기
     */
    public void write2RawDouble(double[] values) {
        if (values.length < 2) {
            throw new IllegalArgumentException("Expected at least 2 values");
        }
        output.writeRawDouble(values[0]);
        output.writeRawDouble(values[1]);
    }

    /**
     * 3D Raw Double (3RD) 쓰기
     */
    public void write3RawDouble(double[] values) {
        if (values.length < 3) {
            throw new IllegalArgumentException("Expected at least 3 values");
        }
        output.writeRawDouble(values[0]);
        output.writeRawDouble(values[1]);
        output.writeRawDouble(values[2]);
    }

    public byte[] toByteArray() {
        return output.toByteArray();
    }

    public long position() {
        return output.position();
    }

    public BitOutput getOutput() {
        return output;
    }
}
