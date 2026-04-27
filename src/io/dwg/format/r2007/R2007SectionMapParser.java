package io.dwg.format.r2007;

import java.util.*;

/**
 * Parse R2007 SectionMap structure
 * Maps section names to page information
 */
public class R2007SectionMapParser {

    public static class SectionMapEntry {
        public String sectionName;
        public int pageId;
        public int size;

        public SectionMapEntry(String name, int pageId, int size) {
            this.sectionName = name;
            this.pageId = pageId;
            this.size = size;
        }

        @Override
        public String toString() {
            return String.format("Section(%s, pageId=0x%X, size=0x%X)", sectionName, pageId, size);
        }
    }

    /**
     * Parse SectionMap from raw bytes
     * Format TBD - likely similar to PageMap but with section names
     */
    public static List<SectionMapEntry> parseSectionMap(byte[] sectionMapData) throws Exception {
        List<SectionMapEntry> sections = new ArrayList<>();

        if (sectionMapData == null || sectionMapData.length < 8) {
            return sections;
        }

        // Extract non-zero LE32 values (same as PageMap)
        List<Long> values = new ArrayList<>();
        for (int offset = 0; offset + 4 <= sectionMapData.length; offset += 4) {
            long val = readLE32(sectionMapData, offset) & 0xFFFFFFFFL;
            if (val != 0) {
                values.add(val);
            }
        }

        // Pair them up: similar to PageMap format
        // Expected: (size, pageId, size, pageId, ...) but with potential section names
        for (int i = 0; i + 1 < values.size(); i += 2) {
            long size = values.get(i);
            long pageId = values.get(i + 1);

            if (size == 0 && pageId == 0) {
                break;
            }

            // For now, use generic section names
            // Real implementation would decode section names from separate data
            String sectionName = "Section_" + (i / 2);

            sections.add(new SectionMapEntry(sectionName, (int) pageId, (int) size));
        }

        return sections;
    }

    private static long readLE32(byte[] data, int offset) {
        if (offset + 4 > data.length) return 0;
        long v1 = data[offset] & 0xFF;
        long v2 = (data[offset + 1] & 0xFF) << 8;
        long v3 = (data[offset + 2] & 0xFF) << 16;
        long v4 = (data[offset + 3] & 0xFF) << 24;
        return v1 | v2 | v3 | v4;
    }
}
