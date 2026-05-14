package io.dwg.test;

import io.dwg.core.util.ReedSolomonDecoder;
import io.dwg.core.util.Lz77Decompressor;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Decompress the RS-decoded header and verify the result
 */
public class DebugRSHeaderDecompressed {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Analyzing Decompressed R2007 Header");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        // Extract and decode RS-encoded header
        byte[] rsEncoded = new byte[0x3d8];
        System.arraycopy(data, 0x80, rsEncoded, 0, 0x3d8);

        byte[] decoded = ReedSolomonDecoder.decodeR2007Data(rsEncoded);
        if (decoded == null) {
            System.out.println("❌ RS decoder failed!");
            return;
        }

        System.out.printf("✅ RS decoded %d bytes\n\n", decoded.length);

        // Extract metadata
        long crc1 = readLE64(decoded, 0);
        long key = readLE64(decoded, 8);
        long crc2 = readLE64(decoded, 16);
        int comprLen = readLE32(decoded, 24);
        int len2 = readLE32(decoded, 28);

        System.out.printf("Metadata:\n");
        System.out.printf("  CRC1:      0x%016X\n", crc1);
        System.out.printf("  Key:       0x%016X\n", key);
        System.out.printf("  CRC2:      0x%016X\n", crc2);
        System.out.printf("  ComprLen:  %d\n", comprLen);
        System.out.printf("  Len2:      %d\n\n", len2);

        if (comprLen == 0) {
            System.out.println("Header is NOT compressed");
            System.out.println("Proceeding to parse as raw Dwg_R2007_Header\n");

            // Analyze uncompressed header
            analyzeHeader(decoded, 32);
            return;
        }

        System.out.println("Header IS compressed (compr_len = " + comprLen + ")\n");

        // Extract compressed data (starts at offset 32)
        System.out.println("Compressed data block:");
        System.out.printf("  Start offset: 32\n");
        System.out.printf("  Compressed size: %d bytes\n", comprLen);
        System.out.printf("  First 32 bytes of compressed data:\n");
        for (int i = 0; i < 32 && (32 + i) < decoded.length; i++) {
            if (i % 16 == 0) System.out.printf("    ");
            System.out.printf("%02X ", decoded[32 + i] & 0xFF);
            if (i % 16 == 15) System.out.println();
        }
        System.out.println("\n");

        // Try to decompress
        byte[] compressed = new byte[comprLen];
        System.arraycopy(decoded, 32, compressed, 0, comprLen);

        System.out.println("Attempting LZ77 decompression...");
        Lz77Decompressor lz77 = new Lz77Decompressor();

        // Spec §5.2 says decompressed size is FIXED at 0x110 = 272 bytes
        int[] sizes = { 272 };

        for (int expectedSize : sizes) {
            System.out.printf("  Trying decompression with size %d...\n", expectedSize);

            try {
                byte[] decompressed = lz77.decompress(compressed, expectedSize);
                System.out.printf("✅ Decompression successful: %d bytes\n\n", decompressed.length);

                // Analyze decompressed header
                analyzeHeader(decompressed, 0);
                System.out.println();

            } catch (Exception e) {
                System.out.println("❌ Decompression failed: " + e.getMessage() + "\n");
            }
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
    }

    private static void analyzeHeader(byte[] data, int offset) {
        System.out.println("Dwg_R2007_Header Analysis (from offset " + offset + "):");
        System.out.println("  Expected fields:");
        System.out.println("    +56 (0x38):  pages_map_offset (LE64)");
        System.out.println("    +80 (0x50):  pages_map_size_comp (LE64)");
        System.out.println("    +88 (0x58):  pages_map_size_uncomp (LE64)");
        System.out.println("    +128 (0x80): sections_map_size_comp (LE64)");
        System.out.println("    +144 (0x90): sections_map_id (LE64)");
        System.out.println("    +152 (0x98): sections_map_size_uncomp (LE64)\n");

        if (offset + 160 <= data.length) {
            long pageMapOffset = readLE64(data, offset + 56);
            long pageMapSizeComp = readLE64(data, offset + 80);
            long pageMapSizeUncomp = readLE64(data, offset + 88);
            long sectionsMapSizeComp = readLE64(data, offset + 128);
            long sectionsMapId = readLE64(data, offset + 144);
            long sectionsMapSizeUncomp = readLE64(data, offset + 152);

            System.out.printf("  pages_map_offset:         0x%X (%d)\n", pageMapOffset, pageMapOffset);
            System.out.printf("  pages_map_size_comp:      0x%X (%d)\n", pageMapSizeComp, pageMapSizeComp);
            System.out.printf("  pages_map_size_uncomp:    0x%X (%d)\n", pageMapSizeUncomp, pageMapSizeUncomp);
            System.out.printf("  sections_map_size_comp:   0x%X (%d)\n", sectionsMapSizeComp, sectionsMapSizeComp);
            System.out.printf("  sections_map_id:          0x%X (%d)\n", sectionsMapId, sectionsMapId);
            System.out.printf("  sections_map_size_uncomp: 0x%X (%d)\n\n", sectionsMapSizeUncomp, sectionsMapSizeUncomp);

            // Sanity checks
            System.out.println("Sanity Checks:");
            boolean pageOffsetOk = pageMapOffset > 0 && pageMapOffset < 0x100000;
            boolean pageComprOk = pageMapSizeComp > 0 && pageMapSizeComp < 0x100000;
            boolean pageUncompOk = pageMapSizeUncomp > 0 && pageMapSizeUncomp < 0x100000;
            boolean secComprOk = sectionsMapSizeComp > 0 && sectionsMapSizeComp < 0x100000;
            boolean secUncompOk = sectionsMapSizeUncomp > 0 && sectionsMapSizeUncomp < 0x100000;

            System.out.printf("  pages_map_offset in range:      %s\n", pageOffsetOk ? "✅" : "❌");
            System.out.printf("  pages_map_size_comp in range:   %s\n", pageComprOk ? "✅" : "❌");
            System.out.printf("  pages_map_size_uncomp in range: %s\n", pageUncompOk ? "✅" : "❌");
            System.out.printf("  sections_map_size_comp:         %s\n", secComprOk ? "✅" : "❌");
            System.out.printf("  sections_map_size_uncomp:       %s\n", secUncompOk ? "✅" : "❌");

            if (pageOffsetOk && pageComprOk && pageUncompOk) {
                System.out.println("\n✅ Header looks valid! PageMap should be at offset 0x" +
                    Long.toHexString(pageMapOffset));
            }
        } else {
            System.out.println("  ❌ Not enough data to read header fields");
        }
    }

    private static long readLE64(byte[] data, int offset) {
        if (offset + 8 > data.length) return 0;
        long v1 = data[offset] & 0xFFL;
        long v2 = (data[offset + 1] & 0xFFL) << 8;
        long v3 = (data[offset + 2] & 0xFFL) << 16;
        long v4 = (data[offset + 3] & 0xFFL) << 24;
        long v5 = (data[offset + 4] & 0xFFL) << 32;
        long v6 = (data[offset + 5] & 0xFFL) << 40;
        long v7 = (data[offset + 6] & 0xFFL) << 48;
        long v8 = (data[offset + 7] & 0xFFL) << 56;
        return v1 | v2 | v3 | v4 | v5 | v6 | v7 | v8;
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
