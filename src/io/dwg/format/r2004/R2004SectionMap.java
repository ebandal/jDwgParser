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

        System.out.printf("[DEBUG] R2004SectionMap.read: sectionMapByteOffset=0x%X\n", sectionMapByteOffset);

        // R2004 section map structure:
        // At sectionMapByteOffset + 0x100:
        //   RL section_type (must be 0x41630e3b)
        //   RL decomp_data_size
        //   RL comp_data_size
        //   RL compression_type
        //   RL checksum
        // Then compressed section map data

        // Seek to section map + 0x100 (page header is at offset 0)
        long actualOffset = (sectionMapByteOffset + 0x100) * 8;
        input.seek(actualOffset);

        // Read page header
        int section_type = input.readRawLong();
        System.out.printf("[DEBUG] R2004SectionMap: section_type=0x%08X\n", section_type);

        if (section_type != 0x41630e3b) {
            System.out.printf("[WARN] R2004SectionMap: Invalid section_type 0x%08X, expected 0x41630e3b\n", section_type);
            return map;
        }

        int decomp_data_size = input.readRawLong();
        int comp_data_size = input.readRawLong();
        int compression_type = input.readRawLong();
        int checksum = input.readRawLong();

        System.out.printf("[DEBUG] R2004SectionMap: decomp_size=%d, comp_size=%d, compression=%d, checksum=0x%X\n",
            decomp_data_size, comp_data_size, compression_type, checksum);

        if (decomp_data_size <= 0 || decomp_data_size > 1000000) {
            System.out.printf("[WARN] R2004SectionMap: unreasonable decomp_data_size %d\n", decomp_data_size);
            return map;
        }

        // Read compressed data
        byte[] compressedData = new byte[comp_data_size];
        for (int i = 0; i < comp_data_size; i++) {
            compressedData[i] = (byte) input.readRawChar();
        }

        System.out.printf("[DEBUG] R2004SectionMap: Read %d bytes of compressed data\n", compressedData.length);

        // DEBUG: Print first 32 bytes of compressed data
        System.out.printf("[DEBUG] R2004SectionMap: First 32 bytes of compressed (hex):\n  ");
        for (int i = 0; i < Math.min(32, compressedData.length); i++) {
            System.out.printf("%02X ", compressedData[i] & 0xFF);
        }
        System.out.println();

        // Decompress using correct R2004 LZ77 algorithm (LibreDWG-based)
        byte[] sectionMapData;
        try {
            io.dwg.core.util.R2004Lz77Decompressor decompressor = new io.dwg.core.util.R2004Lz77Decompressor();
            sectionMapData = decompressor.decompress(compressedData, decomp_data_size);
            System.out.printf("[DEBUG] R2004SectionMap: Decompressed to %d bytes\n", sectionMapData.length);
        } catch (Exception e) {
            System.out.printf("[WARN] R2004SectionMap: Decompression failed: %s\n", e.getMessage());
            e.printStackTrace();
            return map;
        }

        // DEBUG: Print first 256 bytes of decompressed data
        System.out.printf("[DEBUG] R2004SectionMap: First 256 bytes decompressed (hex):\n");
        for (int i = 0; i < Math.min(256, sectionMapData.length); i += 16) {
            System.out.printf("  0x%04X: ", i);
            for (int j = 0; j < 16 && i + j < sectionMapData.length; j++) {
                System.out.printf("%02X ", sectionMapData[i + j] & 0xFF);
            }
            System.out.print("  |");
            for (int j = 0; j < 16 && i + j < sectionMapData.length; j++) {
                byte b = sectionMapData[i + j];
                System.out.print((b >= 32 && b < 127) ? (char)b : '.');
            }
            System.out.println("|");
        }

        // Parse R2004 Section Map: Simple pairs of (SectionID, Size)
        // Each entry is: [4 bytes Section ID] [4 bytes Size]
        // Sections are stored sequentially starting at offset 0x100

        int pos = 0;

        // Map section IDs to canonical names
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

        System.out.printf("[DEBUG] R2004SectionMap: Parsing %d bytes as (SectionID, Size) pairs\n", sectionMapData.length);

        long currentOffset = 0x100; // First section starts at 0x100

        while (pos + 8 <= sectionMapData.length) {
            try {
                int sectionId = (int)(io.dwg.core.util.ByteUtils.readLE32(sectionMapData, pos) & 0xFFFFFFFFL);
                pos += 4;
                long sectionSize = io.dwg.core.util.ByteUtils.readLE32(sectionMapData, pos) & 0xFFFFFFFFL;
                pos += 4;

                String name = sectionNames.getOrDefault(sectionId, "Unknown(" + sectionId + ")");

                System.out.printf("[DEBUG] R2004SectionMap: Section ID=%2d %-25s offset=0x%06X size=0x%X (%d)\n",
                    sectionId, name, currentOffset, sectionSize, sectionSize);

                // Only add non-zero, non-gap sections
                if (sectionSize > 0 && !name.contains("(")) {
                    SectionDescriptor desc = new SectionDescriptor(name);
                    desc.setOffset(currentOffset);
                    desc.setUncompressedSize(sectionSize);
                    map.descriptors.add(desc);
                }

                currentOffset += sectionSize;

            } catch (Exception e) {
                System.out.printf("[ERROR] R2004SectionMap: Failed to parse at offset %d: %s\n", pos, e.getMessage());
                break;
            }
        }

        System.out.printf("[DEBUG] R2004SectionMap: Loaded %d sections\n", map.descriptors.size());

        // Debug: Print all section offsets
        System.out.println("[DEBUG] R2004SectionMap: All section offsets:");
        for (SectionDescriptor desc : map.descriptors) {
            if (desc.offset() > 0) {
                System.out.printf("  %-25s offset=0x%06X\n", desc.name(), desc.offset());
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

    private static String parseUtf16Name(byte[] bytes) {
        int len = 0;
        while (len + 1 < bytes.length && (bytes[len] != 0 || bytes[len + 1] != 0)) {
            len += 2;
        }
        return new String(bytes, 0, len, java.nio.charset.StandardCharsets.UTF_16LE);
    }
}
