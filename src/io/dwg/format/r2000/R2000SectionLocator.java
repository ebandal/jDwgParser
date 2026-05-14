package io.dwg.format.r2000;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitOutput;
import io.dwg.format.common.SectionType;

/**
 * R2000의 섹션 위치 기술자
 * 형식: RC (1바이트) + RL (4바이트) + RL (4바이트) = 9 바이트
 * (R13의 12-byte RL RL RL 형식과는 다름)
 */
public class R2000SectionLocator {
    private int number;
    private long address;
    private long size;

    public R2000SectionLocator(int number, long address, long size) {
        this.number = number;
        this.address = address;
        this.size = size;
    }

    public static R2000SectionLocator read(BitInput input) {
        int number = input.readRawChar() & 0xFF;
        long address = input.readRawLong() & 0xFFFFFFFFL;
        long size = input.readRawLong() & 0xFFFFFFFFL;
        return new R2000SectionLocator(number, address, size);
    }

    public void write(BitOutput output) {
        output.writeRawChar(number & 0xFF);
        output.writeRawLong((int)address);
        output.writeRawLong((int)size);
    }

    public int number() {
        return number;
    }

    public long address() {
        return address;
    }

    public long size() {
        return size;
    }

    // Compatibility aliases
    public int recordNumber() {
        return number;
    }

    public long seeker() {
        return address;
    }

    /**
     * number → SectionType 이름 매핑
     */
    public String toSectionName() {
        return switch (number) {
            case 0 -> SectionType.HEADER.sectionName();
            case 1 -> SectionType.CLASSES.sectionName();
            case 2 -> SectionType.HANDLES.sectionName();
            case 3 -> SectionType.OBJECTS.sectionName();
            default -> "Unknown[" + number + "]";
        };
    }

    @Override
    public String toString() {
        return String.format("R2000SectionLocator[number=%d, address=0x%X, size=0x%X]",
            number, address, size);
    }
}
