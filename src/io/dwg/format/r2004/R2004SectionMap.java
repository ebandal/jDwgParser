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

        // Decompress using R2004 LZ77
        byte[] decompressed;
        try {
            // Try using the Lz77Decompressor from core utilities
            io.dwg.core.util.Lz77Decompressor decompressor = new io.dwg.core.util.Lz77Decompressor();
            decompressed = decompressor.decompress(compressedData, decomp_data_size);
            System.out.printf("[DEBUG] R2004SectionMap: Decompressed to %d bytes\n", decompressed.length);
        } catch (Exception e) {
            System.out.printf("[WARN] R2004SectionMap: Decompression failed: %s\n", e.getMessage());
            e.printStackTrace();
            return map;
        }

        // DEBUG: Print first 64 bytes of decompressed data
        System.out.printf("[DEBUG] R2004SectionMap: First 64 bytes of decompressed (hex+ASCII):\n");
        for (int i = 0; i < Math.min(64, decompressed.length); i += 16) {
            System.out.printf("  0x%04X: ", i);
            for (int j = 0; j < 16 && i + j < decompressed.length; j++) {
                System.out.printf("%02X ", decompressed[i + j] & 0xFF);
            }
            System.out.print("  |");
            for (int j = 0; j < 16 && i + j < decompressed.length; j++) {
                byte b = decompressed[i + j];
                System.out.print((b >= 32 && b < 127) ? (char)b : '.');
            }
            System.out.println("|");
        }

        if (decompressed.length < 20) {
            System.out.printf("[WARN] R2004SectionMap: decompressed data too small for header (%d bytes)\n", decompressed.length);
            return map;
        }

        // Read section map header (20 bytes total):
        // RL num_desc, RL compressed, RLx max_size, RL encrypted, RL num_desc2
        int num_desc = (decompressed[0] & 0xFF) |
                       ((decompressed[1] & 0xFF) << 8) |
                       ((decompressed[2] & 0xFF) << 16) |
                       ((decompressed[3] & 0xFF) << 24);

        System.out.printf("[DEBUG] R2004SectionMap: num_desc=%d (0x%08X)\n", num_desc, num_desc);

        // Sanity check
        if (num_desc <= 0 || num_desc > 100) {
            System.out.printf("[WARN] R2004SectionMap: unreasonable num_desc %d, aborting\n", num_desc);
            return map;
        }

        // Skip header (20 bytes) and read section descriptors
        io.dwg.core.io.ByteBufferBitInput buf =
            new io.dwg.core.io.ByteBufferBitInput(java.nio.ByteBuffer.wrap(decompressed, 20, decompressed.length - 20));

        for (int i = 0; i < num_desc; i++) {
            try {
                SectionDescriptor desc = R2004DataSectionDescriptor.read(buf);
                System.out.printf("[DEBUG] R2004SectionMap: [%d] name='%s'\n", i, desc.name());
                map.descriptors.add(desc);
            } catch (Exception e) {
                System.out.printf("[WARN] R2004SectionMap: Failed to read descriptor [%d]: %s\n", i, e.getMessage());
                break;
            }
        }

        System.out.printf("[DEBUG] R2004SectionMap: Total descriptors loaded: %d\n", map.descriptors.size());
        return map;
    }

    public List<SectionDescriptor> descriptors() {
        return descriptors;
    }

    public Optional<SectionDescriptor> find(String name) {
        return descriptors.stream().filter(d -> d.name().equals(name)).findFirst();
    }
}
