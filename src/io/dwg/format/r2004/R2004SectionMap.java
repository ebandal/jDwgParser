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

        // DEBUG: Print ALL decompressed data
        System.out.printf("[DEBUG] R2004SectionMap: ALL %d bytes decompressed (hex):\n", sectionMapData.length);
        for (int i = 0; i < sectionMapData.length; i += 16) {
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

        // Parse section descriptors (same format as R2007)
        int pos = 0;

        if (sectionMapData.length < 4) {
            System.out.printf("[WARN] R2004SectionMap: Data too small for section count\n");
            return map;
        }

        int sectionCount = (int)(io.dwg.core.util.ByteUtils.readLE32(sectionMapData, pos) & 0xFFFFFFFFL);
        pos += 4;
        System.out.printf("[DEBUG] R2004SectionMap: sectionCount=%d\n", sectionCount);

        for (int i = 0; i < sectionCount && i < 100; i++) {
            if (pos + 6 * 4 + 64 > sectionMapData.length) {
                System.out.printf("[WARN] R2004SectionMap: Buffer overrun at section %d\n", i);
                break;
            }

            try {
                long dataSize = io.dwg.core.util.ByteUtils.readLE32(sectionMapData, pos) & 0xFFFFFFFFL;
                pos += 4;
                long maxDecompressedSize = io.dwg.core.util.ByteUtils.readLE32(sectionMapData, pos) & 0xFFFFFFFFL;
                pos += 4;
                long compressionType = io.dwg.core.util.ByteUtils.readLE32(sectionMapData, pos) & 0xFFFFFFFFL;
                pos += 4;
                // 3 more RL fields (reserved)
                pos += 4;
                pos += 4;
                pos += 4;

                // Section name (64 bytes UTF-16LE)
                byte[] nameBytes = new byte[64];
                System.arraycopy(sectionMapData, pos, nameBytes, 0, 64);
                pos += 64;
                String name = parseUtf16Name(nameBytes);

                // Page count
                int pageCount = (int)(io.dwg.core.util.ByteUtils.readLE32(sectionMapData, pos) & 0xFFFFFFFFL);
                pos += 4;

                System.out.printf("[DEBUG] R2004SectionMap: Section %d: \"%s\" (pageCount=%d, dataSize=0x%X, decomp=0x%X, compression=%d)\n",
                    i, name.isEmpty() ? "(empty)" : name, pageCount, dataSize, maxDecompressedSize, compressionType);

                SectionDescriptor desc = new SectionDescriptor(name);
                desc.setCompressedSize(dataSize);
                desc.setUncompressedSize(maxDecompressedSize);
                desc.setCompressionType((int) compressionType);

                // Parse page descriptors (pageId, dataSize, pageOffset in order)
                for (int j = 0; j < pageCount && j < 100; j++) {
                    if (pos + 12 > sectionMapData.length) {
                        System.out.printf("[WARN] R2004SectionMap: Buffer overrun at page %d of section %d\n", j, i);
                        break;
                    }

                    long pageId = io.dwg.core.util.ByteUtils.readLE32(sectionMapData, pos) & 0xFFFFFFFFL;
                    pos += 4;
                    long pageDataSize = io.dwg.core.util.ByteUtils.readLE32(sectionMapData, pos) & 0xFFFFFFFFL;
                    pos += 4;
                    long pageOffset = io.dwg.core.util.ByteUtils.readLE32(sectionMapData, pos) & 0xFFFFFFFFL;
                    pos += 4;

                    desc.addPage(new io.dwg.format.common.PageInfo(pageOffset, pageDataSize, pageId));
                    System.out.printf("  [Page %d] id=0x%X, size=0x%X, offset=0x%X\n", j, pageId, dataSize, pageOffset);
                }

                map.descriptors.add(desc);

            } catch (Exception e) {
                System.out.printf("[ERROR] R2004SectionMap: Failed to parse section %d: %s\n", i, e.getMessage());
                e.printStackTrace();
                break;
            }
        }

        System.out.printf("[DEBUG] R2004SectionMap: Loaded %d sections\n", map.descriptors.size());
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
