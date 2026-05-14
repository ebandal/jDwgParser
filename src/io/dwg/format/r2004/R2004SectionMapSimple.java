package io.dwg.format.r2004;

import io.dwg.core.io.BitInput;
import io.dwg.format.common.SectionDescriptor;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * R2004 Section Map 단순 파서 - ID → Offset 매핑
 * LibreDWG과 다른 접근: section map은 섹션 ID와 파일 오프셋의 매핑
 */
public class R2004SectionMapSimple {
    private final Map<Integer, Long> sectionOffsets = new HashMap<>();
    private final List<SectionDescriptor> descriptors = new ArrayList<>();

    private R2004SectionMapSimple() {}

    public static R2004SectionMapSimple read(byte[] decompressedData) {
        R2004SectionMapSimple map = new R2004SectionMapSimple();

        if (decompressedData.length < 16) {
            System.out.printf("[WARN] R2004SectionMapSimple: data too small (%d bytes)\n", decompressedData.length);
            return map;
        }

        // Header: 16 bytes
        // 0x00-0x03: num_desc (RL)
        // 0x04-0x0F: reserved fields (3 x RL)

        int num_desc = (decompressedData[0] & 0xFF) |
                       ((decompressedData[1] & 0xFF) << 8) |
                       ((decompressedData[2] & 0xFF) << 16) |
                       ((decompressedData[3] & 0xFF) << 24);

        System.out.printf("[DEBUG] R2004SectionMapSimple: num_desc=%d\n", num_desc);

        // After header (offset 16), parse section ID-offset pairs
        // Each pair: 4 bytes section_id + 4 bytes offset
        int offset = 16;
        int pairCount = 0;

        while (offset + 8 <= decompressedData.length && pairCount < 100) {
            // Debug: print raw bytes
            System.out.printf("[DEBUG] R2004SectionMapSimple: offset=0x%X: bytes=%02X %02X %02X %02X %02X %02X %02X %02X\n",
                offset,
                decompressedData[offset] & 0xFF,
                decompressedData[offset+1] & 0xFF,
                decompressedData[offset+2] & 0xFF,
                decompressedData[offset+3] & 0xFF,
                decompressedData[offset+4] & 0xFF,
                decompressedData[offset+5] & 0xFF,
                decompressedData[offset+6] & 0xFF,
                decompressedData[offset+7] & 0xFF);

            int sectionId = (decompressedData[offset] & 0xFF) |
                           ((decompressedData[offset+1] & 0xFF) << 8) |
                           ((decompressedData[offset+2] & 0xFF) << 16) |
                           ((decompressedData[offset+3] & 0xFF) << 24);

            long sectionOffset = ((decompressedData[offset+4] & 0xFF) |
                                 ((decompressedData[offset+5] & 0xFF) << 8) |
                                 ((decompressedData[offset+6] & 0xFF) << 16) |
                                 ((decompressedData[offset+7] & 0xFF) << 24)) & 0xFFFFFFFFL;

            System.out.printf("[DEBUG] R2004SectionMapSimple: Section %d @ 0x%X\n", sectionId, sectionOffset);

            map.sectionOffsets.put(sectionId, sectionOffset);
            offset += 8;
            pairCount++;
        }

        System.out.printf("[DEBUG] R2004SectionMapSimple: Loaded %d sections\n", map.sectionOffsets.size());
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
