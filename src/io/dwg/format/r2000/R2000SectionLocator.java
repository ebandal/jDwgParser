package io.dwg.format.r2000;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitOutput;
import io.dwg.format.common.SectionType;

/**
 * R2000의 섹션 위치 기술자 (R13과 동일한 구조)
 */
public class R2000SectionLocator {
    private int recordNumber;
    private long seeker;
    private long size;

    public R2000SectionLocator(int recordNumber, long seeker, long size) {
        this.recordNumber = recordNumber;
        this.seeker = seeker;
        this.size = size;
    }

    public static R2000SectionLocator read(BitInput input) {
        long recordNumber = input.readRawLong() & 0xFFFFFFFFL;
        long seeker = input.readRawLong() & 0xFFFFFFFFL;
        long size = input.readRawLong() & 0xFFFFFFFFL;
        return new R2000SectionLocator((int)recordNumber, seeker, size);
    }

    public void write(BitOutput output) {
        output.writeRawLong(recordNumber);
        output.writeRawLong((int)seeker);
        output.writeRawLong((int)size);
    }

    public int recordNumber() {
        return recordNumber;
    }

    public long seeker() {
        return seeker;
    }

    public long size() {
        return size;
    }

    /**
     * recordNumber → SectionType 이름 매핑
     */
    public String toSectionName() {
        return switch (recordNumber) {
            case 0 -> SectionType.HEADER.sectionName();
            case 1 -> SectionType.CLASSES.sectionName();
            case 2 -> SectionType.HANDLES.sectionName();
            case 3 -> SectionType.OBJECTS.sectionName();
            default -> "Unknown[" + recordNumber + "]";
        };
    }

    @Override
    public String toString() {
        return String.format("R2000SectionLocator[record=%d, offset=%d, size=%d]",
            recordNumber, seeker, size);
    }
}
