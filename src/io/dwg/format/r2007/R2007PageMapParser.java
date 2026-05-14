package io.dwg.format.r2007;

import io.dwg.core.util.Lz77Decompressor;
import java.util.*;

/**
 * Parse R2007 PageMap structure
 * Spec §5 - Maps page IDs to offsets within the data section
 */
public class R2007PageMapParser {

    public static class PageMapEntry {
        public int pageId;
        public int size;

        public PageMapEntry(int pageId, int size) {
            this.pageId = pageId;
            this.size = size;
        }

        @Override
        public String toString() {
            return String.format("Page(id=0x%X, size=0x%X)", pageId, size);
        }
    }

    /**
     * Parse PageMap from raw bytes (uncompressed)
     * Format: Alternating LE32 values with zero padding
     * Structure: val1(LE32) | 00 00 00 00 | val2(LE32) | 00 00 00 00 | ...
     * Non-zero values are paired: (size, pageId), (size, pageId), ...
     *
     * Determined from Arc.dwg: values are stored as
     * 0x400, 0x17, 0x400, 0x18, 0xA0, 0x3, 0x6DC0, 0x4, ...
     * Which pairs as: (0x400, 0x17), (0x400, 0x18), (0xA0, 0x3), (0x6DC0, 0x4), ...
     */
    public static List<PageMapEntry> parsePageMap(byte[] pageMapData) throws Exception {
        List<PageMapEntry> pages = new ArrayList<>();

        if (pageMapData == null || pageMapData.length < 4) {
            return pages;
        }

        // First, extract all non-zero LE32 values
        java.util.List<Long> values = new java.util.ArrayList<>();
        for (int offset = 0; offset + 4 <= pageMapData.length; offset += 4) {
            long val = readLE32(pageMapData, offset) & 0xFFFFFFFFL;
            if (val != 0) {
                values.add(val);
            }
        }

        // Pair them up: odd indices are sizes, even indices are pageIds
        // So values[0] = size, values[1] = pageId, values[2] = size, values[3] = pageId, ...
        for (int i = 0; i + 1 < values.size(); i += 2) {
            long size = values.get(i);
            long pageId = values.get(i + 1);

            if (size == 0 && pageId == 0) {
                break; // End marker
            }

            pages.add(new PageMapEntry((int) pageId, (int) size));
        }

        return pages;
    }

    /**
     * Extract PageMap data from file at given offset
     */
    public static byte[] extractPageMapData(byte[] fileData, long pageMapFileOffset,
                                           int pageMapSizeComp, int pageMapSizeUncomp,
                                           boolean isCompressed) throws Exception {
        if (pageMapFileOffset + pageMapSizeComp > fileData.length) {
            throw new Exception("PageMap data extends beyond file");
        }

        byte[] pageMapBytes = new byte[pageMapSizeComp];
        System.arraycopy(fileData, (int)pageMapFileOffset, pageMapBytes, 0, pageMapSizeComp);

        if (isCompressed) {
            Lz77Decompressor decompressor = new Lz77Decompressor();
            return decompressor.decompress(pageMapBytes, pageMapSizeUncomp);
        }
        return pageMapBytes;
    }

    private static int readLE32(byte[] data, int offset) {
        if (offset + 4 > data.length) return 0;
        int v1 = data[offset] & 0xFF;
        int v2 = (data[offset + 1] & 0xFF) << 8;
        int v3 = (data[offset + 2] & 0xFF) << 16;
        int v4 = (data[offset + 3] & 0xFF) << 24;
        return v1 | v2 | v3 | v4;
    }
}
