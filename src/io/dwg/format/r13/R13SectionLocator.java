package io.dwg.format.r13;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitOutput;
import io.dwg.format.common.SectionType;

/**
 * A section-locator record of the R13–R2000 file header (spec §3.2.6): record number (raw
 * byte), seeker (raw long, byte address in the file) and size (raw long).
 */
public class R13SectionLocator {
    private final int recordNumber;
    private final long seeker;
    private final long size;

    public R13SectionLocator(int recordNumber, long seeker, long size) {
        this.recordNumber = recordNumber;
        this.seeker = seeker;
        this.size = size;
    }

    public static R13SectionLocator read(BitInput input) {
        int number = input.readRawChar();
        long address = input.readRawLong() & 0xFFFFFFFFL;
        long length = input.readRawLong() & 0xFFFFFFFFL;
        return new R13SectionLocator(number, address, length);
    }

    /**
     * Writes the record. The record number is written as a raw long, which matches the record
     * size {@code DwgWriter} assumes but not §3.2.6 (raw byte); to be settled when writing is finished.
     */
    public void write(BitOutput output) {
        output.writeRawLong(recordNumber);
        output.writeRawLong((int) seeker);
        output.writeRawLong((int) size);
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
     * The section a record number stands for (§3.2.6): 0 header variables, 1 classes,
     * 2 object map, 3 the special table of R13 C3 and later (object free space), 4 template.
     * The specification does not describe record 5; it is taken as the auxiliary header.
     */
    public String toSectionName() {
        switch (recordNumber) {
            case 0: return SectionType.HEADER.sectionName();
            case 1: return SectionType.CLASSES.sectionName();
            case 2: return SectionType.HANDLES.sectionName();
            case 3: return SectionType.OBJ_FREE_SPACE.sectionName();
            case 4: return SectionType.TEMPLATE.sectionName();
            case 5: return SectionType.AUX_HEADER.sectionName();
            default: return "Unknown[" + recordNumber + "]";
        }
    }

    @Override
    public String toString() {
        return "R13SectionLocator[record=" + recordNumber + ", offset=" + seeker + ", size=" + size + "]";
    }
}
