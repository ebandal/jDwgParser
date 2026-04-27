package io.dwg.format.r2007;

import java.util.*;

/**
 * Parse R2007 SectionMap structure using RLL-encoded values
 * Per libredwg decode_r2007.c: read_sections_map() function
 */
public class R2007SectionMapParser {

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
        }

        @Override
        public String toString() {
            return String.format("Section(%s, dataSize=0x%X, maxSize=0x%X, numPages=%d)",
                sectionName, dataSize, maxSize, numPages);
        }
    }

    /**
     * Parse SectionMap from decompressed bytes
     * Each section entry consists of:
     * - 8 RLL (64-bit LE) values (64 bytes total):
     *   1. data_size
     *   2. max_size
     *   3. encrypted
     *   4. hashcode
     *   5. name_length
     *   6. unknown
     *   7. encoded
     *   8. num_pages
     * - Followed by name_length bytes of section name (UTF-8 or UTF-16LE)
     */
    public static List<SectionMapEntry> parseSectionMap(byte[] sectionMapData) throws Exception {
        List<SectionMapEntry> sections = new ArrayList<>();

        if (sectionMapData == null || sectionMapData.length < 64) {
            return sections;
        }

        int offset = 0;
        int sectionIndex = 0;

        while (offset + 64 <= sectionMapData.length) {
            long dataSize = readRLL(sectionMapData, offset);
            offset += 8;
            long maxSize = readRLL(sectionMapData, offset);
            offset += 8;
            long encrypted = readRLL(sectionMapData, offset);
            offset += 8;
            long hashcode = readRLL(sectionMapData, offset);
            offset += 8;
            long nameLength = readRLL(sectionMapData, offset);
            offset += 8;
            long unknown = readRLL(sectionMapData, offset);
            offset += 8;
            long encoded = readRLL(sectionMapData, offset);
            offset += 8;
            long numPages = readRLL(sectionMapData, offset);
            offset += 8;

            // Stop on invalid name_length (sanity check from libredwg)
            if (nameLength < 0 || nameLength >= 48) {
                break;
            }

            // Extract section name (UTF-8 bytes)
            String sectionName = "Section_" + sectionIndex;
            if (nameLength > 0 && offset + nameLength <= sectionMapData.length) {
                byte[] nameBytes = new byte[(int) nameLength];
                System.arraycopy(sectionMapData, offset, nameBytes, 0, (int) nameLength);
                try {
                    sectionName = new String(nameBytes, "UTF-8").trim();
                } catch (Exception e) {
                    // Fallback to generic name if UTF-8 decode fails
                    sectionName = "Section_" + sectionIndex;
                }
                offset += (int) nameLength;
            }

            sections.add(new SectionMapEntry(sectionName, dataSize, maxSize, encrypted,
                hashcode, nameLength, unknown, encoded, numPages));

            sectionIndex++;
        }

        return sections;
    }

    private static long readRLL(byte[] data, int offset) {
        if (offset + 8 > data.length) {
            return 0;
        }
        // Read as little-endian 64-bit value
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value |= ((long)(data[offset + i] & 0xFF)) << (i * 8);
        }
        return value;
    }
}
