package io.dwg.format.common;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitOutput;

/**
 * 공통 로직을 제공하는 기반 클래스
 */
public abstract class AbstractFileStructureHandler implements DwgFileStructureHandler {

    /**
     * SentinelValidator 위임
     */
    protected void validateSentinel(byte[] actual, byte[] expected, String ctx) {
        io.dwg.core.util.SentinelValidator.validate(actual, expected, ctx);
    }

    /**
     * CRC 검증
     */
    protected void validateCrc(byte[] data, int expected, String ctx) {
        io.dwg.core.util.CrcCalculator.Crc32Calculator crc32 = 
            new io.dwg.core.util.CrcCalculator.Crc32Calculator();
        int calculated = crc32.calculate(data, 0);
        if (calculated != expected) {
            throw new io.dwg.core.exception.DwgCorruptedException(ctx, 
                String.format("CRC mismatch: expected=0x%08X, calculated=0x%08X", expected, calculated));
        }
    }

    /**
     * count 바이트 순차 읽기
     */
    protected byte[] readBytes(BitInput input, int count) {
        byte[] result = new byte[count];
        for (int i = 0; i < count; i++) {
            result[i] = (byte)input.readRawChar();
        }
        return result;
    }

    /**
     * 바이트 배열 순차 쓰기
     */
    protected void writeBytes(BitOutput output, byte[] data) {
        for (byte b : data) {
            output.writeRawChar(b & 0xFF);
        }
    }
}
