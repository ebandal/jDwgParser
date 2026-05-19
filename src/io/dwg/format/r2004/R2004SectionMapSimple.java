package io.dwg.format.r2004;

import io.dwg.format.common.SectionDescriptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * R2004 Section Map 단순 파서 - ID → Offset 매핑
 */
public class R2004SectionMapSimple {
    private final Map<Integer, Long> sectionOffsets = new HashMap<>();
    private final List<SectionDescriptor> descriptors = new ArrayList<>();

    private R2004SectionMapSimple() {}

    public static R2004SectionMapSimple read(byte[] decompressedData) {
        R2004SectionMapSimple map = new R2004SectionMapSimple();

        if (decompressedData.length < 16) {
            return map;
        }

        // Header: 16 bytes
        // 0x00-0x03: num_desc (RL)
        // 0x04-0x0F: reserved fields (3 x RL)

        int offset = 16;
        int pairCount = 0;

        while (offset + 8 <= decompressedData.length && pairCount < 100) {
            int sectionId = (decompressedData[offset] & 0xFF) |
                           ((decompressedData[offset+1] & 0xFF) << 8) |
                           ((decompressedData[offset+2] & 0xFF) << 16) |
                           ((decompressedData[offset+3] & 0xFF) << 24);

            long sectionOffset = ((decompressedData[offset+4] & 0xFF) |
                                 ((decompressedData[offset+5] & 0xFF) << 8) |
                                 ((decompressedData[offset+6] & 0xFF) << 16) |
                                 ((decompressedData[offset+7] & 0xFF) << 24)) & 0xFFFFFFFFL;

            map.sectionOffsets.put(sectionId, sectionOffset);
            offset += 8;
            pairCount++;
        }

        return map;
    }

    public Map<Integer, Long> sectionOffsets() {
        return sectionOffsets;
    }

    public Optional<Long> offsetForSection(int sectionId) {
        return Optional.ofNullable(sectionOffsets.get(sectionId));
    }

    public List<SectionDescriptor> descriptors() {
        return descriptors;
    }
}
