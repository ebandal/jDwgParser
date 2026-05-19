package io.dwg.format.r2004;

import io.dwg.core.io.BitInput;
import io.dwg.format.common.SectionDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * §4 Section Map 페이지 파싱. 섹션 이름과 페이지 목록 구성.
 */
public class R2004SectionMap {
    private final List<SectionDescriptor> descriptors = new ArrayList<>();

    private R2004SectionMap() {}

    public static R2004SectionMap read(BitInput input, long sectionMapByteOffset) throws Exception {
        R2004SectionMap map = new R2004SectionMap();

        // R2004 section map structure:
        // At sectionMapByteOffset + 0x100:
        //   RL section_type (must be 0x41630e3b)
        //   RL decomp_data_size
        //   RL comp_data_size
        //   RL compression_type
        //   RL checksum
        // Then compressed section map data

        long pageHeaderOffset = sectionMapByteOffset + 0x100;
        long actualOffset = pageHeaderOffset * 8;
        input.seek(actualOffset);

        byte[] pageHeader = new byte[32];
        for (int i = 0; i < 32; i++) {
            pageHeader[i] = (byte) input.readRawChar();
        }

        long page_type = io.dwg.core.util.ByteUtils.readLE32(pageHeader, 0);

        if (page_type != 0x41630e3bL) {
            return map;
        }

        long comp_data_size = io.dwg.core.util.ByteUtils.readLE32(pageHeader, 8);
        long decomp_data_size = io.dwg.core.util.ByteUtils.readLE32(pageHeader, 28);

        if (decomp_data_size <= 0 || decomp_data_size > 1000000) {
            return map;
        }

        byte[] compressedData = new byte[(int)comp_data_size];
        for (int i = 0; i < comp_data_size; i++) {
            compressedData[i] = (byte) input.readRawChar();
        }

        byte[] sectionMapData;
        try {
            io.dwg.core.util.R2004Lz77Decompressor decompressor = new io.dwg.core.util.R2004Lz77Decompressor();
            sectionMapData = decompressor.decompress(compressedData, (int)decomp_data_size);
        } catch (Exception e) {
            return map;
        }

        java.util.Map<Integer, String> sectionNames = new java.util.HashMap<>();
        sectionNames.put(0, "(Empty)");
        sectionNames.put(1, "AcDb:Header");
        sectionNames.put(2, "AcDb:AuxHeader");
        sectionNames.put(3, "AcDb:Classes");
        sectionNames.put(4, "AcDb:Handles");
        sectionNames.put(5, "AcDb:Template");
        sectionNames.put(6, "AcDb:ObjFreeSpace");
        sectionNames.put(7, "(Gap)");
        sectionNames.put(8, "AcDb:RevHistory");
        sectionNames.put(9, "AcDb:Security");
        sectionNames.put(10, "AcDb:SummaryInfo");
        sectionNames.put(11, "AcDb:VBAProject");
        sectionNames.put(12, "(Gap)");
        sectionNames.put(13, "AcDb:Objects");
        sectionNames.put(14, "AcDb:SecdInfo");
        sectionNames.put(15, "(Gap)");
        sectionNames.put(16, "(Gap)");
        sectionNames.put(19, "(Gap)");
        sectionNames.put(20, "(Gap)");
        sectionNames.put(21, "AcDb:AppInfo");
        sectionNames.put(27, "AcDb:Preview");
        sectionNames.put(28, "AcDb:AppInfoHistory");

        int pos = 0;
        while (pos + 8 <= sectionMapData.length) {
            try {
                byte[] entryBytes = new byte[8];
                for (int i = 0; i < 8; i++) {
                    entryBytes[i] = sectionMapData[pos + i];
                }

                int sectionId = (entryBytes[0] & 0xFF) | ((entryBytes[1] & 0xFF) << 8);
                pos += 8;

                String name = sectionNames.getOrDefault(sectionId, "Unknown(" + sectionId + ")");

                if (sectionId >= 0 && sectionId <= 28) {
                    SectionDescriptor desc = new SectionDescriptor(name);
                    desc.setOffset(0);
                    desc.setUncompressedSize(0);
                    map.descriptors.add(desc);
                }

            } catch (Exception e) {
                break;
            }
        }

        return map;
    }

    public List<SectionDescriptor> descriptors() {
        return descriptors;
    }

    public Optional<SectionDescriptor> find(String name) {
        return descriptors.stream().filter(d -> d.name().equals(name)).findFirst();
    }

}
