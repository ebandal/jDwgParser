package io.dwg.core.io;

/**
 * 비트 단위 쓰기 계약을 정의하는 최상위 인터페이스.
 */
public interface BitOutput {
    /**
     * 1비트 쓰기
     */
    void writeBit(boolean bit);

    /**
     * value의 하위 n비트 쓰기 (MSB first)
     */
    void writeBits(int value, int n);

    /**
     * 비압축 1바이트 쓰기
     */
    void writeRawChar(int v);

    /**
     * 비압축 2바이트 little-endian 쓰기
     */
    void writeRawShort(short v);

    /**
     * 비압축 4바이트 little-endian 쓰기
     */
    void writeRawLong(int v);

    /**
     * 비압축 8바이트 IEEE 754 쓰기
     */
    void writeRawDouble(double v);

    /**
     * 쓴 내용을 바이트 배열로 반환
     */
    byte[] toByteArray();

    /**
     * 현재 비트 위치 반환
     */
    long position();
}
