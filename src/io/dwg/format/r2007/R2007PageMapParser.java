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
     * Structure: sequence of (pageId: LE32, size: LE32) pairs
     */
    public static List<PageMapEntry> parsePageMap(byte[] pageMapData) throws Exception {
        List<PageMapEntry> pages = new ArrayList<>();

        if (pageMapData == null || pageMapData.length < 8) {
            return pages;
        }

        for (int offset = 0; offset + 8 <= pageMapData.length; offset += 8) {
            int pageId = readLE32(pageMapData, offset);
            int size = readLE32(pageMapData, offset + 4);

            // Check for end marker or invalid entries
            if (pageId == 0 && size == 0) {
                break;
            }

            pages.add(new PageMapEntry(pageId, size));
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
