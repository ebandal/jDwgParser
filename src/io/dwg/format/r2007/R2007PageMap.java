package io.dwg.format.r2007;

import io.dwg.core.util.Lz77Decompressor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * R2007 페이지 맵. 페이지 ID → 파일 오프셋 매핑.
 */
public class R2007PageMap {
    private final Map<Long, Long> pageOffsets = new HashMap<>();

    private R2007PageMap() {}

    public static R2007PageMap read(byte[] compressed, long expectedUncompressed) throws Exception {
        R2007PageMap map = new R2007PageMap();

        byte[] data;
        if (compressed.length < expectedUncompressed) {
            Lz77Decompressor lz77 = new Lz77Decompressor();
            data = lz77.decompress(compressed, (int) expectedUncompressed);
        } else {
            data = compressed;
        }

        // 각 항목: pageId(RL=4) + offset(RL=4)
        int pos = 0;
        long runningOffset = 0x480; // 헤더 이후 시작 오프셋 (R2007 기본값)
        while (pos + 8 <= data.length) {
            long pageId = readLE32(data, pos);  pos += 4;
            long size   = readLE32(data, pos);  pos += 4;
            if (pageId > 0) {
                map.pageOffsets.put(pageId, runningOffset);
            }
            runningOffset += size;
        }

        return map;
    }

    public Optional<Long> offsetForPage(long pageId) {
        return Optional.ofNullable(pageOffsets.get(pageId));
    }

    public List<Long> pageIds() {
        return new ArrayList<>(pageOffsets.keySet());
    }

    private static long readLE32(byte[] data, int off) {
        return ((long)(data[off] & 0xFF))
             | ((long)(data[off+1] & 0xFF) << 8)
             | ((long)(data[off+2] & 0xFF) << 16)
             | ((long)(data[off+3] & 0xFF) << 24);
    }
}
