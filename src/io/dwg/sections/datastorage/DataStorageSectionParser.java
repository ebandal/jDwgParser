package io.dwg.sections.datastorage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The data storage section AcDb:AcDsPrototype_1b of R2013+ files (spec chapter 24): a byte
 * stream of file segments. This reads the data records of its data segments, each keyed by the
 * handle of the object it belongs to. For an ACIS entity (REGION, 3DSOLID) the record is the
 * entity's model as a SAB stream ("ACIS BinaryFile" in the R2013 samples, "ASM BinaryFile4" in
 * the R2018 ones); the samples also have PNG images under the handles of LAYOUT and VIEW objects.
 * <pre>
 *   file header (§24.1): signature, header size, unknown, version, unknown, revision,
 *     segment index offset, unknown, segment index entry count, schema index segment index,
 *     data index segment index, search segment index, previous save index, file size (Int32 each)
 *   each file segment (§24.2.1): signature Int16, name 6 bytes, segment index, unknown,
 *     segment size, unknown, revision, unknown, system data alignment offset, object data
 *     alignment offset (Int32 each), 8 alignment bytes
 *   segment index (§24.2.2.1): per entry an offset UInt64 and a size UInt32
 *   data index (§24.2.2.2): entry count, unknown, per entry segment index, local offset and
 *     schema index (UInt32 each; segment index 0 is a stub)
 *   data segment (§24.2.2.3): at each local offset a record header (entry size UInt32,
 *     unknown UInt32, handle UInt64, local offset UInt32); at each record's local offset from
 *     the data start, its size UInt32 and the bytes, or 0xBB106BB1 and a blob reference
 *     (§24.2.2.3.1) to pages in blob01 segments (§24.2.2.4)
 * </pre>
 * Settled on the samples: the local offsets of the data index are counted from the end of the
 * data segment's header (the specification says from the segment's start), and the data start
 * is the segment's object data alignment offset shifted left 4 bits, from the segment's start.
 * The blob reference form is read as the specification gives it; the samples have none.
 */
public final class DataStorageSectionParser {

    private static final int SEGMENT_HEADER_SIZE = 48;
    private static final int RECORD_HEADER_SIZE = 20;
    private static final long BLOB_REFERENCE = 0xBB106BB1L;

    private DataStorageSectionParser() {
    }

    /** The data records of the section, by object handle, in the order of the data index. */
    public static Map<Long, byte[]> parse(byte[] section) {
        ByteBuffer b = ByteBuffer.wrap(section).order(ByteOrder.LITTLE_ENDIAN);
        int segmentIndexOffset = b.getInt(24);
        int segmentCount = b.getInt(32);
        int dataIndexSegment = b.getInt(40);
        if (segmentCount < 0 || segmentCount > section.length / 12) {
            throw new IllegalStateException("bad segment index entry count " + segmentCount);
        }
        io.dwg.core.io.ReadLimits.reserve((long) (segmentCount) * 8L, "DataStorageSectionParser");
        long[] segmentOffsets = new long[segmentCount];
        for (int i = 0; i < segmentCount; i++) {
            segmentOffsets[i] = b.getLong(segmentIndexOffset + SEGMENT_HEADER_SIZE + 12 * i);
        }

        Map<Long, byte[]> records = new LinkedHashMap<>();
        int dataIndex = segmentStart(segmentOffsets, dataIndexSegment, section) + SEGMENT_HEADER_SIZE;
        int entries = b.getInt(dataIndex);
        for (int i = 0; i < entries; i++) {
            int entry = dataIndex + 8 + 12 * i;
            int segment = b.getInt(entry);
            if (segment == 0) {
                continue;
            }
            int start = segmentStart(segmentOffsets, segment, section);
            int header = start + SEGMENT_HEADER_SIZE + b.getInt(entry + 4);
            int dataStart = start + (b.getInt(start + 36) << 4);
            int segmentEnd = start + b.getInt(start + 16);
            long handle = b.getLong(header + 8);
            int record = dataStart + b.getInt(header + 16);
            if (header + RECORD_HEADER_SIZE > dataStart || record < dataStart || record + 4 > segmentEnd) {
                throw new IllegalStateException("data record of handle " + Long.toHexString(handle) + " is outside its segment");
            }
            long size = b.getInt(record) & 0xFFFFFFFFL;
            if (size == BLOB_REFERENCE) {
                records.put(handle, readBlob(b, segmentOffsets, record + 4, section));
            } else if (record + 4 + size <= segmentEnd) {
                records.put(handle, Arrays.copyOfRange(section, record + 4, record + 4 + (int) size));
            } else {
                throw new IllegalStateException("data record of handle " + Long.toHexString(handle) + " runs past its segment");
            }
        }
        return records;
    }

    /** A blob reference record (§24.2.2.3.1) and its pages (§24.2.2.4). */
    private static byte[] readBlob(ByteBuffer b, long[] segmentOffsets, int at, byte[] section) {
        long total = b.getLong(at);
        int pages = b.getInt(at + 8);
        if (total < 0 || total > section.length || pages < 0 || pages > section.length) {
            throw new IllegalStateException("bad blob reference: " + total + " bytes in " + pages + " pages");
        }
        io.dwg.core.io.ReadLimits.reserve((long) (total), "DataStorageSectionParser");
        byte[] data = new byte[(int) total];
        for (int i = 0; i < pages; i++) {
            int page = segmentStart(segmentOffsets, b.getInt(at + 32 + 8 * i), section) + SEGMENT_HEADER_SIZE;
            long offset = b.getLong(page + 8);
            long size = b.getLong(page + 24);
            if (offset < 0 || size < 0 || offset + size > total || page + 32 + size > section.length) {
                throw new IllegalStateException("bad blob page " + i);
            }
            System.arraycopy(section, page + 32, data, (int) offset, (int) size);
        }
        return data;
    }

    private static int segmentStart(long[] offsets, int index, byte[] section) {
        if (index < 0 || index >= offsets.length || offsets[index] <= 0
                || offsets[index] + SEGMENT_HEADER_SIZE > section.length) {
            throw new IllegalStateException("no file segment " + index);
        }
        return (int) offsets[index];
    }
}
