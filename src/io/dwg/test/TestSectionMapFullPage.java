package io.dwg.test;

import io.dwg.core.util.Lz77Decompressor;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Test: Try LZ77 on full page data (4864 bytes) instead of just 905
 * Maybe the entire page is compressed to fit in allocated space
 */
public class TestSectionMapFullPage {
    public static void main(String[] args) throws Exception {
        byte[] fileData = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));

        long pageOffset = 0xFC80;
        int pageSize = 0x1300; // 4864 bytes
        int sizeUncomp = 2188;

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Test: LZ77 on full SectionMap page (4864 bytes)");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        Lz77Decompressor lz77 = new Lz77Decompressor();

        byte[] pageData = new byte[pageSize];
        System.arraycopy(fileData, (int)pageOffset, pageData, 0, pageSize);

        System.out.printf("Attempting LZ77 decompression of full %d-byte page:\n\n", pageSize);

        try {
            byte[] result = lz77.decompress(pageData, sizeUncomp);
            System.out.printf("✅ SUCCESS!\n");
            System.out.printf("   Input: %d bytes\n", pageSize);
            System.out.printf("   Output: %d bytes (expected %d)\n", result.length, sizeUncomp);

            // Show as PageMap-style entries
            System.out.println("\nParsing as entries:");
            java.util.List<String> entries = new java.util.ArrayList<>();
            for (int i = 0; i + 8 <= result.length; i += 8) {
                int val1 = readLE32(result, i);
                int val2 = readLE32(result, i + 4);
                if (val1 == 0 && val2 == 0) break;
                entries.add(String.format("0x%08X, 0x%08X", val1, val2));
            }

            for (int i = 0; i < Math.min(15, entries.size()); i++) {
                System.out.println("  [" + i + "] " + entries.get(i));
            }
            if (entries.size() > 15) {
                System.out.println("  ... and " + (entries.size() - 15) + " more");
            }
            System.out.println("\nTotal entries: " + entries.size());

        } catch (Exception e) {
            System.out.printf("❌ Failed: %s\n\n", e.getMessage());

            System.out.println("Trying different expected output sizes:\n");
            for (int trySize : new int[]{320, 905, 1024, 2188, 4096, 4864}) {
                try {
                    byte[] result = lz77.decompress(pageData, trySize);
                    System.out.printf("✅ Size %d: SUCCESS! Decompressed to %d bytes\n", trySize, result.length);
                    break;
                } catch (Exception e2) {
                    // Show first 30 chars of error
                    String msg = e2.getMessage();
                    System.out.printf("❌ Size %d: %s\n", trySize, msg.substring(0, Math.min(50, msg.length())));
                }
            }
        }
    }

    static int readLE32(byte[] data, int offset) {
        if (offset + 4 > data.length) return 0;
        int v1 = data[offset] & 0xFF;
        int v2 = (data[offset + 1] & 0xFF) << 8;
        int v3 = (data[offset + 2] & 0xFF) << 16;
        int v4 = (data[offset + 3] & 0xFF) << 24;
        return v1 | v2 | v3 | v4;
    }
}
