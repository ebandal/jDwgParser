package io.dwg.core.io;

/**
 * 비트 단위 읽기 계약을 정의하는 최상위 인터페이스.
 */
public interface BitInput {
    /**
     * 1비트 읽기
     */
    boolean readBit();

    /**
     * n비트를 읽어 int로 반환 (MSB first)
     * n ≤ 32
     */
    int readBits(int n);

    /**
     * 비압축 1바이트(RC) 읽기
     */
    int readRawChar();

    /**
     * 비압축 2바이트(RS) little-endian 읽기
     */
    short readRawShort();

    /**
     * 비압축 4바이트(RL) little-endian 읽기
     */
    int readRawLong();

    /**
     * 비압축 8바이트(RD) IEEE 754 읽기
     */
    double readRawDouble();

    /**
     * 현재 비트 위치 반환
     */
    long position();

    /**
     * 지정 비트 위치로 이동
     */
    void seek(long bitPos);

    /**
     * 스트림 끝 여부
     */
    boolean isEof();
}
