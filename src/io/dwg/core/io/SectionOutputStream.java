package io.dwg.core.io;

import io.dwg.core.version.DwgVersion;

/**
 * 섹션 쓰기용 스트림. 완성 후 바이트 배열 추출.
 */
public class SectionOutputStream {
    private ByteBufferBitOutput output;
    private String sectionName;

    public SectionOutputStream(String sectionName) {
        this.sectionName = sectionName;
        this.output = new ByteBufferBitOutput();
    }

    /**
     * 이 섹션용 BitStreamWriter 생성
     */
    public BitStreamWriter writer(DwgVersion ver) {
        return new BitStreamWriter(output, ver);
    }

    /**
     * 완성된 섹션 바이트 반환
     */
    public byte[] toByteArray() {
        return output.toByteArray();
    }

    /**
     * 섹션 이름 반환
     */
    public String sectionName() {
        return sectionName;
    }

    public BitOutput getBitOutput() {
        return output;
    }
}
