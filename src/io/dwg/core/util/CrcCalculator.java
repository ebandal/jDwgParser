package io.dwg.core.util;

/**
 * CRC 계산 기반 추상 클래스
 */
public abstract class CrcCalculator {
    
    /**
     * data에 대해 seed 기반 CRC 계산
     */
    public abstract int calculate(byte[] data, int seed);

    /**
     * 계산값과 기대값 비교
     */
    public boolean verify(byte[] data, int expectedCrc) {
        return calculate(data, 0) == expectedCrc;
    }

    /**
     * CRC-32 검증 (R2004+ 헤더)
     */
    public static class Crc32Calculator extends CrcCalculator {
        @Override
        public int calculate(byte[] data, int seed) {
            int crc = seed ^ 0xFFFFFFFF;
            for (byte b : data) {
                crc = CrcLookupTables.CRC32_TABLE[(crc ^ b) & 0xFF] ^ (crc >>> 8);
            }
            return crc ^ 0xFFFFFFFF;
        }

        public int calculateForSection(byte[] data) {
            return calculate(data, 0);
        }
    }

    /**
     * CRC-8 검증 (R13-R15 객체 맵)
     */
    public static class Crc8Calculator extends CrcCalculator {
        @Override
        public int calculate(byte[] data, int seed) {
            int crc = seed;
            for (byte b : data) {
                crc = CrcLookupTables.CRC8_TABLE[(crc ^ (b & 0xFF)) & 0xFF];
            }
            return crc & 0xFF;
        }
    }
}
