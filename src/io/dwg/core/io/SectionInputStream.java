package io.dwg.core.io;

import io.dwg.core.version.DwgVersion;

/**
 * 특정 DWG 섹션 데이터를 감싸는 스트림.
 * 섹션 범위 밖 읽기 방지 및 오프셋 추적.
 */
public class SectionInputStream {
    private ByteBufferBitInput input;
    private String sectionName;
    private byte[] rawData;

    public SectionInputStream(byte[] data, String sectionName) {
        this.rawData = data;
        this.sectionName = sectionName;
        this.input = new ByteBufferBitInput(data);
    }

    /**
     * 이 섹션용 BitStreamReader 생성
     */
    public BitStreamReader reader(DwgVersion ver) {
        return new BitStreamReader(input, ver);
    }

    /**
     * 섹션 바이트 크기 반환
     */
    public int size() {
        return rawData.length;
    }

    /**
     * 섹션 이름 반환
     */
    public String sectionName() {
        return sectionName;
    }

    /**
     * 원본 바이트 배열 반환
     */
    public byte[] rawBytes() {
        return rawData;
    }

    /**
     * 스트림 재설정
     */
    public void reset() {
        this.input = new ByteBufferBitInput(rawData);
    }

    /**
     * 스트림 위치 반환
     */
    public long getPosition() {
        return input.position();
    }

    /**
     * 스트림 위치 설정
     */
    public void setPosition(long bitPos) {
        input.seek(bitPos);
    }

    public BitInput getBitInput() {
        return input;
    }
}
