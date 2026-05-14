package io.dwg.core.util;

public abstract class CrcLookupTables {
    /**
     * CRC-32 테이블 (0x04C11DB7 다항식)
     */
    public static final int[] CRC32_TABLE = new int[256];
    
    /**
     * CRC-8 테이블 (DWG 스펙 기준)
     */
    static final int[] CRC8_TABLE = new int[256];

    static {
        // CRC-32 테이블 초기화
        for (int i = 0; i < 256; i++) {
            int crc = i;
            for (int j = 0; j < 8; j++) {
                if ((crc & 1) != 0) {
                    crc = (crc >>> 1) ^ 0xEDB88320;
                } else {
                    crc = crc >>> 1;
                }
            }
            CRC32_TABLE[i] = crc;
        }

        // CRC-8 테이블 초기화 (DWG 스펙 §23 기반)
        for (int i = 0; i < 256; i++) {
            int crc = i;
            for (int j = 0; j < 8; j++) {
                int flag = (crc & 0x80) != 0 ? 1 : 0;
                crc = (crc << 1) & 0xFF;
                if (flag != 0) {
                    crc ^= 0x8F;
                }
            }
            CRC8_TABLE[i] = crc;
        }
    }
}
