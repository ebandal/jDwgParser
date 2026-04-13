package io.dwg.format.r2007;

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
        int sectionCount = (int) readLE32(decompressedData, pos); pos += 4;

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
        if (pos + 6 * 8 + 64 > data.length) return pos;

        long dataSize           = readLE64(data, pos); pos += 8;
        long maxDecompressedSize= readLE64(data, pos); pos += 8;
        long unknown            = readLE64(data, pos); pos += 8;  // compression_type
        int  compressionType    = (int)(unknown & 0xFFFFFFFFL);
        pos += 8; // sectionId  – 예약
        pos += 8; // encrypted  – 예약
        pos += 8; // unknown2   – 예약

        // 이름 (64바이트 UTF-16LE)
        byte[] nameBytes = new byte[64];
        System.arraycopy(data, pos, nameBytes, 0, 64); pos += 64;
        String name = decodeUtf16Name(nameBytes);

        // 페이지 수
        if (pos + 4 > data.length) return pos;
        int pageCount = (int) readLE32(data, pos); pos += 4;

        SectionDescriptor desc = new SectionDescriptor(name);
        desc.setCompressedSize(dataSize);
        desc.setUncompressedSize(maxDecompressedSize);
        desc.setCompressionType(compressionType);

        for (int j = 0; j < pageCount; j++) {
            if (pos + 16 > data.length) break;
            long pageId     = readLE64(data, pos); pos += 8;
            long pageSize   = readLE64(data, pos); pos += 8;
            desc.addPage(new PageInfo(0, pageSize, pageId));
        }

        map.descriptors.add(desc);
        return pos;
    }

    private static String decodeUtf16Name(byte[] bytes) {
        int len = 0;
        while (len + 1 < bytes.length && (bytes[len] != 0 || bytes[len+1] != 0)) len += 2;
        return new String(bytes, 0, len, StandardCharsets.UTF_16LE);
    }

    public List<SectionDescriptor> descriptors() { return descriptors; }

    public Optional<SectionDescriptor> find(String name) {
        return descriptors.stream().filter(d -> d.name().equals(name)).findFirst();
    }

    private static long readLE32(byte[] d, int o) {
        return ((long)(d[o]&0xFF)) | ((long)(d[o+1]&0xFF)<<8)
             | ((long)(d[o+2]&0xFF)<<16) | ((long)(d[o+3]&0xFF)<<24);
    }
    private static long readLE64(byte[] d, int o) {
        return readLE32(d, o) | (readLE32(d, o+4) << 32);
    }
}
