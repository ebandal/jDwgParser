package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersionDetector;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.DwgFileStructureHandlerFactory;
import io.dwg.format.common.DwgFileStructureHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Test the LZ77 decompression fix for R2007+ files.
 * Expected: After fix, R2007+ files should return non-empty section map.
 */
public class TestLz77DecompressionFix {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Testing LZ77 Decompression Fix for R2007+");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        // Test R2007+ files
        List<Path> r2007Files = Files.walk(Paths.get("./samples"))
            .filter(p -> p.toString().endsWith(".dwg"))
            .filter(p -> {
                try {
                    DwgVersion v = DwgVersionDetector.detect(Files.readAllBytes(p));
                    String vstr = v.toString();
                    return vstr.contains("2007") || vstr.contains("2010") ||
                           vstr.contains("2013") || vstr.contains("2018");
                } catch (Exception e) {
                    return false;
                }
            })
            .sorted()
            .collect(Collectors.toList());

        System.out.printf("Testing %d R2007+ files\n\n", r2007Files.size());

        int successCount = 0;
        int failureCount = 0;
        int totalSections = 0;

        // Test first 10 R2007+ files
        for (Path file : r2007Files.stream().limit(10).collect(Collectors.toList())) {
            try {
                byte[] data = Files.readAllBytes(file);
                DwgVersion version = DwgVersionDetector.detect(data);

                System.out.printf("Testing: %s (%s)\n", file.getFileName(), version);

                BitInput input = new ByteBufferBitInput(data);
                DwgFileStructureHandler handler = DwgFileStructureHandlerFactory.forVersion(version);

                var header = handler.readHeader(input);

                ByteBufferBitInput inputForSections = new ByteBufferBitInput(data);
                Map<String, SectionInputStream> sections = handler.readSections(inputForSections, header);

                System.out.printf("  Sections found: %d", sections.size());

                if (sections.isEmpty()) {
                    System.out.println(" ❌ FAILED (expected > 0)");
                    failureCount++;
                } else {
                    System.out.println(" ✅ SUCCESS");
                    successCount++;
                    totalSections += sections.size();

                    for (String name : sections.keySet()) {
                        System.out.printf("    - %s\n", name);
                    }
                }
                System.out.println();

            } catch (Exception e) {
                System.out.printf("  Error: %s\n\n", e.getMessage());
                failureCount++;
            }
        }

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.printf("Results: %d success, %d failures\n", successCount, failureCount);
        System.out.printf("Total sections found: %d\n", totalSections);

        if (successCount > 0) {
            System.out.println("✅ LZ77 Decompression Fix WORKING!");
        } else {
            System.out.println("❌ LZ77 Decompression Fix FAILED");
        }
        System.out.println("═══════════════════════════════════════════════════════════════");
    }
}
