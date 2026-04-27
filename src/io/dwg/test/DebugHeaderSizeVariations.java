package io.dwg.test;

import io.dwg.core.util.ReedSolomonDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Try different decompressed sizes to find valid Dwg_R2007_Header
 */
public class DebugHeaderSizeVariations {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));

        // Extract and decode RS-encoded header
        byte[] rsEncoded = new byte[0x3d8];
        System.arraycopy(data, 0x80, rsEncoded, 0, 0x3d8);

        byte[] decoded = ReedSolomonDecoder.decodeR2007Data(rsEncoded);
        if (decoded == null) {
            System.out.println("RS decoder failed");
            return;
        }

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Testing different header interpretations");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        // Option 1: Interpret bytes 32-255 as raw Dwg_R2007_Header (uncompressed)
        System.out.println("Option 1: Raw Dwg_R2007_Header (bytes 32-255 uncompressed)");
        System.out.println("─────────────────────────────────────────────────────────────");
        analyzeHeader(decoded, 32);

        // Option 2: Interpret bytes 32-193 + padding as Dwg_R2007_Header
        System.out.println("\n\nOption 2: Only compressed bytes (32-193) as header");
        System.out.println("─────────────────────────────────────────────────────────────");
        byte[] headerPart = new byte[224];
        int comprLen = readLE32(decoded, 24);
        System.arraycopy(decoded, 32, headerPart, 0, Math.min(comprLen, 224));
        analyzeHeader(headerPart, 0);

        // Option 3: Check if any LE64 in the metadata area points to PageMap
        System.out.println("\n\nOption 3: Check metadata for PageMap offset");
        System.out.println("─────────────────────────────────────────────────────────────");
        long crc1 = readLE64(decoded, 0);
        long key = readLE64(decoded, 8);
        long crc2 = readLE64(decoded, 16);
        int comprLen2 = readLE32(decoded, 24);
        int len2 = readLE32(decoded, 28);

        System.out.printf("CRC1:      0x%016X\n", crc1);
        System.out.printf("Key:       0x%016X\n", key);
        System.out.printf("CRC2:      0x%016X\n", crc2);
        System.out.printf("ComprLen:  %d (0x%X)\n", comprLen2, comprLen2);
        System.out.printf("Len2:      %d\n\n", len2);

        // Check if any of these look like file offsets
        if (crc1 > 0x400 && crc1 < 0x100000) {
            System.out.printf("⚠️  CRC1 looks like offset: 0x%X\n", crc1);
        }
        if (key > 0x400 && key < 0x100000) {
            System.out.printf("⚠️  Key looks like offset: 0x%X\n", key);
        }
    }

    private static void analyzeHeader(byte[] data, int offset) {
        System.out.println("Expected fields:");
        System.out.println("  +56 (0x38):  pages_map_offset");
        System.out.println("  +80 (0x50):  pages_map_size_comp");
        System.out.println("  +88 (0x58):  pages_map_size_uncomp");
        System.out.println("  +128 (0x80): sections_map_size_comp");
        System.out.println("  +144 (0x90): sections_map_id");
        System.out.println("  +152 (0x98): sections_map_size_uncomp\n");

        if (offset + 160 > data.length) {
            System.out.printf("❌ Not enough data (need %d, have %d)\n", offset + 160, data.length);
            return;
        }

        long pageMapOffset = readLE64(data, offset + 56);
        long pageMapSizeComp = readLE64(data, offset + 80);
        long pageMapSizeUncomp = readLE64(data, offset + 88);
        long sectionsMapSizeComp = readLE64(data, offset + 128);
        long sectionsMapId = readLE64(data, offset + 144);
        long sectionsMapSizeUncomp = readLE64(data, offset + 152);

        System.out.printf("pages_map_offset:         0x%X (%d)\n", pageMapOffset, pageMapOffset);
        System.out.printf("pages_map_size_comp:      0x%X (%d)\n", pageMapSizeComp, pageMapSizeComp);
        System.out.printf("pages_map_size_uncomp:    0x%X (%d)\n", pageMapSizeUncomp, pageMapSizeUncomp);
        System.out.printf("sections_map_size_comp:   0x%X (%d)\n", sectionsMapSizeComp, sectionsMapSizeComp);
        System.out.printf("sections_map_id:          0x%X (%d)\n", sectionsMapId, sectionsMapId);
        System.out.printf("sections_map_size_uncomp: 0x%X (%d)\n", sectionsMapSizeUncomp, sectionsMapSizeUncomp);

        boolean pageOffsetOk = pageMapOffset > 0x400 && pageMapOffset < 0x100000;
        boolean pageComprOk = pageMapSizeComp > 0 && pageMapSizeComp < 0x100000;
        boolean pageUncompOk = pageMapSizeUncomp > 0 && pageMapSizeUncomp < 0x100000;
        boolean secComprOk = sectionsMapSizeComp > 0 && sectionsMapSizeComp < 0x100000;
        boolean secUncompOk = sectionsMapSizeUncomp > 0 && sectionsMapSizeUncomp < 0x100000;

        System.out.printf("\nSanity Checks:\n");
        System.out.printf("  pages_map_offset in range:      %s\n", pageOffsetOk ? "✅" : "❌");
        System.out.printf("  pages_map_size_comp in range:   %s\n", pageComprOk ? "✅" : "❌");
        System.out.printf("  pages_map_size_uncomp in range: %s\n", pageUncompOk ? "✅" : "❌");
        System.out.printf("  sections_map_size_comp in range:%s\n", secComprOk ? "✅" : "❌");
        System.out.printf("  sections_map_size_uncomp in range:%s\n", secUncompOk ? "✅" : "❌");

        if (pageOffsetOk && pageComprOk && pageUncompOk) {
            System.out.printf("\n✅ PageMap offset is VALID! Should be at 0x%X\n", pageMapOffset);
        }
    }

    private static long readLE64(byte[] data, int offset) {
        if (offset + 8 > data.length) return 0;
        return (data[offset] & 0xFFL) |
               ((data[offset + 1] & 0xFFL) << 8) |
               ((data[offset + 2] & 0xFFL) << 16) |
               ((data[offset + 3] & 0xFFL) << 24) |
               ((data[offset + 4] & 0xFFL) << 32) |
               ((data[offset + 5] & 0xFFL) << 40) |
               ((data[offset + 6] & 0xFFL) << 48) |
               ((data[offset + 7] & 0xFFL) << 56);
    }

    private static int readLE32(byte[] data, int offset) {
        if (offset + 4 > data.length) return 0;
        return (data[offset] & 0xFF) |
               ((data[offset + 1] & 0xFF) << 8) |
               ((data[offset + 2] & 0xFF) << 16) |
               ((data[offset + 3] & 0xFF) << 24);
    }
}
