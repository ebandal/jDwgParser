package io.dwg.core.util;

import io.dwg.core.io.BitInput;

/**
 * 바이트 및 비트 읽기/쓰기 유틸리티.
 * Little-endian 정수 변환 함수를 중앙화.
 */
public final class ByteUtils {
    private ByteUtils() {}

    // =========================================================
    // BitInput에서 읽기
    // =========================================================

    /**
     * BitInput에서 16비트 little-endian 정수 읽기
     */
    public static int readLE16(BitInput in) {
        return (in.readRawChar() & 0xFF) | ((in.readRawChar() & 0xFF) << 8);
    }

    /**
     * BitInput에서 32비트 little-endian 정수 읽기
     */
    public static long readLE32(BitInput in) {
        long v = 0;
        for (int i = 0; i < 4; i++) {
            v |= ((long)(in.readRawChar() & 0xFF)) << (i * 8);
        }
        return v;
    }

    /**
     * BitInput에서 64비트 little-endian 정수 읽기
     */
    public static long readLE64(BitInput in) {
        long v = 0;
        for (int i = 0; i < 8; i++) {
            v |= ((long)(in.readRawChar() & 0xFF)) << (i * 8);
        }
        return v;
    }

    // =========================================================
    // 바이트 배열에서 읽기
    // =========================================================

    /**
     * 바이트 배열에서 16비트 little-endian 정수 읽기
     */
    public static int readLE16(byte[] data, int offset) {
        if (offset + 1 >= data.length) return 0;
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }

    /**
     * 바이트 배열에서 32비트 little-endian 정수 읽기
     */
    public static long readLE32(byte[] data, int offset) {
        if (offset + 3 >= data.length) return 0;
        return ((long)(data[offset] & 0xFF))
             | ((long)(data[offset + 1] & 0xFF) << 8)
             | ((long)(data[offset + 2] & 0xFF) << 16)
             | ((long)(data[offset + 3] & 0xFF) << 24);
    }

    /**
     * 바이트 배열에서 64비트 little-endian 정수 읽기
     */
    public static long readLE64(byte[] data, int offset) {
        return readLE32(data, offset) | (readLE32(data, offset + 4) << 32);
    }

    // =========================================================
    // 바이트 배열에 쓰기
    // =========================================================

    /**
     * 바이트 배열에 16비트 little-endian 정수 쓰기
     */
    public static void writeLE16(byte[] data, int offset, int value) {
        if (offset + 1 >= data.length) return;
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    /**
     * 바이트 배열에 32비트 little-endian 정수 쓰기
     */
    public static void writeLE32(byte[] data, int offset, long value) {
        if (offset + 3 >= data.length) return;
        for (int i = 0; i < 4; i++) {
            data[offset + i] = (byte) ((value >> (i * 8)) & 0xFF);
        }
    }

    /**
     * 바이트 배열에 64비트 little-endian 정수 쓰기
     */
    public static void writeLE64(byte[] data, int offset, long value) {
        if (offset + 7 >= data.length) return;
        for (int i = 0; i < 8; i++) {
            data[offset + i] = (byte) ((value >> (i * 8)) & 0xFF);
        }
    }
}
