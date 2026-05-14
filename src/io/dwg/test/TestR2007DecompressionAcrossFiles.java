package io.dwg.test;

import io.dwg.core.util.ReedSolomonDecoder;
import io.dwg.core.util.Lz77Decompressor;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.DirectoryStream;
import java.util.*;

/**
 * Test LZ77 decompression across multiple R2007 sample files
 */
public class TestR2007DecompressionAcrossFiles {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Testing R2007 Decompression Across Multiple Files");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        List<Path> r2007Files = new ArrayList<>();
        Path r2007Dir = Paths.get("./samples/2007");

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(r2007Dir, "*.dwg")) {
            for (Path path : stream) {
                r2007Files.add(path);
            }
        }

        Collections.sort(r2007Files);
        System.out.printf("Found %d R2007 files\n\n", r2007Files.size());

        int successCount = 0;
        int failureCount = 0;
        Map<String, String> results = new LinkedHashMap<>();

        for (Path filePath : r2007Files) {
            System.out.printf("Testing: %s\n", filePath.getFileName());

            try {
                byte[] data = Files.readAllBytes(filePath);

                // Extract and decode RS-encoded header
                byte[] rsEncoded = new byte[0x3d8];
                System.arraycopy(data, 0x80, rsEncoded, 0, 0x3d8);

                byte[] decoded = ReedSolomonDecoder.decodeR2007Data(rsEncoded);
                if (decoded == null) {
                    System.out.println("  ❌ RS decoder failed");
                    failureCount++;
                    results.put(filePath.getFileName().toString(), "RS FAILED");
                    System.out.println();
                    continue;
                }

                // Read metadata
                int comprLen = readLE32(decoded, 24);

                if (comprLen == 0) {
                    System.out.println("  ✅ Not compressed, skipping decompression");
                    successCount++;
                    results.put(filePath.getFileName().toString(), "NOT COMPRESSED");
                    System.out.println();
                    continue;
                }

                // Try decompression
                byte[] compressed = new byte[comprLen];
                System.arraycopy(decoded, 32, compressed, 0, comprLen);

                Lz77Decompressor lz77 = new Lz77Decompressor();
                byte[] decompressed = lz77.decompress(compressed, 272);

                System.out.printf("  ✅ Decompressed: %d bytes\n", decompressed.length);

                // Analyze decompressed header
                long pageMapOffset = readLE64(decompressed, 56);
                long pageMapSizeComp = readLE64(decompressed, 80);
                long pageMapSizeUncomp = readLE64(decompressed, 88);

                System.out.printf("    pages_map_offset:       0x%X\n", pageMapOffset);
                System.out.printf("    pages_map_size_comp:    0x%X\n", pageMapSizeComp);
                System.out.printf("    pages_map_size_uncomp:  0x%X\n", pageMapSizeUncomp);

                // Check if values are reasonable
                boolean offsetOk = pageMapOffset > 0 && pageMapOffset < 0x100000;
                boolean compOk = pageMapSizeComp > 0 && pageMapSizeComp < 0x100000;
                boolean uncompOk = pageMapSizeUncomp > 0 && pageMapSizeUncomp < 0x100000;

                if (offsetOk && compOk && uncompOk) {
                    System.out.println("  ✅ Header values look valid");
                    successCount++;
                    results.put(filePath.getFileName().toString(), String.format("OK (offset=0x%X)", pageMapOffset));
                } else {
                    System.out.printf("  ⚠️  Values outside valid ranges (offset=%s, comp=%s, uncomp=%s)\n",
                        offsetOk ? "✅" : "❌", compOk ? "✅" : "❌", uncompOk ? "✅" : "❌");
                    failureCount++;
                    results.put(filePath.getFileName().toString(),
                        String.format("GARBAGE (offset=0x%X)", pageMapOffset));
                }

            } catch (Exception e) {
                System.out.printf("  ❌ Error: %s\n", e.getMessage());
                failureCount++;
                results.put(filePath.getFileName().toString(), "ERROR: " + e.getMessage());
            }
            System.out.println();
        }

        // Summary
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("SUMMARY");
        System.out.println("═══════════════════════════════════════════════════════════════\n");
        System.out.printf("Success: %d\n", successCount);
        System.out.printf("Failure: %d\n\n", failureCount);

        System.out.println("Results by file:");
        for (Map.Entry<String, String> entry : results.entrySet()) {
            System.out.printf("  %-30s %s\n", entry.getKey(), entry.getValue());
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
