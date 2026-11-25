package io.dwg.format.r2007;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the decoded and decompressed R2007 section map.
 *
 * <p>Written from the Open Design Specification for .dwg files, section 5.2 (the section map
 * table that follows "Next, the Section Map should be loaded"). The map is a sequence of
 * sections until the data ends. All numbers are 64-bit little-endian. Each section is:
 * <pre>
 *   data size, max size, encryption, hash code, name length, unknown, encoding, page count
 *   name (UTF-16LE)
 *   page count × (data offset, page size, page id, uncompressed size, compressed size,
 *                 checksum, CRC)
 * </pre>
 * In the files the name length is the number of bytes of the name, including its terminating
 * zero character.
 */
public class R2007SectionMapParser {

    private static final int SECTION_HEADER_SIZE = 8 * 8;
    private static final int PAGE_ENTRY_SIZE = 7 * 8;
    private static final int MAX_NAME_LENGTH = 255;

    /** One page of a section. */
    public static class SectionPageEntry {
        public long offset;
        public long size;
        public long id;
        public long uncompSize;
        public long compSize;
        public long checksum;
        public long crc;

        public SectionPageEntry(long offset, long size, long id, long uncompSize,
                                long compSize, long checksum, long crc) {
            this.offset = offset;
            this.size = size;
            this.id = id;
            this.uncompSize = uncompSize;
            this.compSize = compSize;
            this.checksum = checksum;
            this.crc = crc;
        }
    }

    /** One section and its pages. */
    public static class SectionMapEntry {
        public String sectionName;
        public long dataSize;
        public long maxSize;
        public long encrypted;
        public long hashcode;
        public long nameLength;
        public long unknown;
        public long encoded;
        public long numPages;
        public List<SectionPageEntry> pages;

        public SectionMapEntry(String name, long dataSize, long maxSize, long encrypted,
                               long hashcode, long nameLength, long unknown, long encoded, long numPages) {
            this.sectionName = name;
            this.dataSize = dataSize;
            this.maxSize = maxSize;
            this.encrypted = encrypted;
            this.hashcode = hashcode;
            this.nameLength = nameLength;
            this.unknown = unknown;
            this.encoded = encoded;
            this.numPages = numPages;
            this.pages = new ArrayList<>();
        }

        @Override
        public String toString() {
            return String.format("Section(%s, dataSize=0x%X, maxSize=0x%X, numPages=%d, pageData=%d)",
                sectionName, dataSize, maxSize, numPages, pages.size());
        }
    }

    /**
     * Parses the section map. Parsing stops at the first section whose name or pages do not fit
     * in the remaining data.
     */
    public static List<SectionMapEntry> parseSectionMap(byte[] data) {
        List<SectionMapEntry> sections = new ArrayList<>();
        if (data == null) {
            return sections;
        }

        int pos = 0;
        while (pos + SECTION_HEADER_SIZE <= data.length) {
            long dataSize = le64(data, pos);
            long maxSize = le64(data, pos + 8);
            long encryption = le64(data, pos + 16);
            long hashCode = le64(data, pos + 24);
            long nameLength = le64(data, pos + 32);
            long unknown = le64(data, pos + 40);
            long encoding = le64(data, pos + 48);
            long pageCount = le64(data, pos + 56);
            pos += SECTION_HEADER_SIZE;

            long remaining = data.length - pos;
            if (nameLength < 0 || nameLength > MAX_NAME_LENGTH || nameLength > remaining
                    || pageCount < 0 || pageCount > (remaining - nameLength) / PAGE_ENTRY_SIZE) {
                break;
            }

            String name = new String(data, pos, (int) nameLength, StandardCharsets.UTF_16LE);
            int end = name.indexOf('\0');
            if (end >= 0) {
                name = name.substring(0, end);
            }
            name = name.trim();
            if (nameLength == 0) {
                name = "Section_" + sections.size();
            }
            pos += (int) nameLength;

            SectionMapEntry section = new SectionMapEntry(name, dataSize, maxSize, encryption,
                hashCode, nameLength, unknown, encoding, pageCount);
            for (long p = 0; p < pageCount; p++) {
                section.pages.add(new SectionPageEntry(
                    le64(data, pos), le64(data, pos + 8), le64(data, pos + 16), le64(data, pos + 24),
                    le64(data, pos + 32), le64(data, pos + 40), le64(data, pos + 48)));
                pos += PAGE_ENTRY_SIZE;
            }
            sections.add(section);
        }
        return sections;
    }

    private static long le64(byte[] data, int at) {
        long value = 0;
        for (int i = 7; i >= 0; i--) {
            value = (value << 8) | (data[at + i] & 0xFF);
        }
        return value;
    }
}
