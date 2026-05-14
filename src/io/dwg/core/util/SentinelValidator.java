package io.dwg.core.util;

/**
 * 섹션 경계의 16바이트 Sentinel 검증
 */
public class SentinelValidator {
    
    /**
     * Sentinel 검증
     */
    public static void validate(byte[] actual, byte[] expected, String context) {
        if (actual == null || expected == null || actual.length != expected.length) {
            throw new IllegalArgumentException("Invalid sentinel length");
        }
        
        for (int i = 0; i < expected.length; i++) {
            if (actual[i] != expected[i]) {
                throw new io.dwg.core.exception.DwgCorruptedException(context, 
                    "Sentinel mismatch at byte " + i);
            }
        }
    }

    /**
     * 바이트 비트 반전 (end sentinel 생성에 사용)
     */
    public static byte[] invertSentinel(byte[] sentinel) {
        byte[] inverted = new byte[sentinel.length];
        for (int i = 0; i < sentinel.length; i++) {
            inverted[i] = (byte)~sentinel[i];
        }
        return inverted;
    }

    /**
     * 헤더 시작 sentinel (§3)
     * "DWG\0\0\0"
     */
    public static final byte[] HEADER_SENTINEL = new byte[]{
        (byte)0x6D, (byte)0x61, (byte)0x69,
        (byte)0x00, (byte)0x00, (byte)0x00
    };
}
