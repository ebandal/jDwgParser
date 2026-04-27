package io.dwg.format.r2007;

import io.dwg.core.util.ByteUtils;
import io.dwg.format.common.PageInfo;
import io.dwg.format.common.SectionDescriptor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * R2007 섹션 맵. 섹션명 → 페이지 목록 매핑. §5.4 파싱.
 */
public class R2007SectionMap {
    private final List<SectionDescriptor> descriptors = new ArrayList<>();

    private R2007SectionMap() {}

    public static R2007SectionMap read(byte[] decompressedData) {
        R2007SectionMap map = new R2007SectionMap();
        int pos = 0;

        if (decompressedData.length < 4) return map;
        int sectionCount = (int) ByteUtils.readLE32(decompressedData, pos); pos += 4;

        for (int i = 0; i < sectionCount; i++) {
            if (pos + 64 > decompressedData.length) break;
            try {
                pos = parseSectionDescriptor(decompressedData, pos, map);
            } catch (Exception e) {
                break;
            }
        }
        return map;
    }

    private static int parseSectionDescriptor(byte[] data, int pos, R2007SectionMap map) {
        // Libredwg format (R2007+):
        // - data_size (RLL = LE64)
        // - max_size (RLL = LE64)
        // - encrypted (RLL = LE64)
        // - hashcode (RLL = LE64)
        // - name_length (RLL = LE64) ← length of UTF-16LE name in BYTES
        // - unknown (RLL = LE64)
        // - encoded (RLL = LE64)
        // - num_pages (RLL = LE64)
        // Then:
        // - name (UTF-16LE, name_length bytes)
        // - pages (page entries, 3 × LE64 per page)

        if (pos + 64 > data.length) return pos;  // At least 8 RLL fields

        long dataSize        = ByteUtils.readLE64(data, pos); pos += 8;
        long maxSize         = ByteUtils.readLE64(data, pos); pos += 8;
        long encrypted       = ByteUtils.readLE64(data, pos); pos += 8;
        long hashcode        = ByteUtils.readLE64(data, pos); pos += 8;
        long nameLength      = ByteUtils.readLE64(data, pos); pos += 8;
        long unknown         = ByteUtils.readLE64(data, pos); pos += 8;
        long encoded         = ByteUtils.readLE64(data, pos); pos += 8;
        long numPages        = ByteUtils.readLE64(data, pos); pos += 8;

        // Read section name (UTF-16LE, nameLength bytes)
        String name = "";
        if (nameLength > 0 && nameLength < 512 && pos + nameLength <= data.length) {
            name = new String(data, pos, (int)nameLength, StandardCharsets.UTF_16LE);
            pos += (int)nameLength;
        }

        SectionDescriptor desc = new SectionDescriptor(name);
        desc.setCompressedSize(dataSize);
        desc.setUncompressedSize(maxSize);
        // encoded field likely indicates compression type
        desc.setCompressionType((int)(encoded & 0xFFFFFFFFL));

        // Read page entries (3 × LE64 fields per page)
        for (int j = 0; j < numPages && j < 1000; j++) {
            if (pos + 24 > data.length) break;
            long pageId     = ByteUtils.readLE64(data, pos); pos += 8;
            long pageSize   = ByteUtils.readLE64(data, pos); pos += 8;
            long pageOffset = ByteUtils.readLE64(data, pos); pos += 8;  // offset within section
            desc.addPage(new PageInfo(0, pageSize, pageId));
        }

        map.descriptors.add(desc);
        return pos;
    }

    public List<SectionDescriptor> descriptors() { return descriptors; }

    public Optional<SectionDescriptor> find(String name) {
        return descriptors.stream().filter(d -> d.name().equals(name)).findFirst();
    }

}
