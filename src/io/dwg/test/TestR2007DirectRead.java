package io.dwg.test;

import io.dwg.core.version.DwgVersionDetector;
import io.dwg.format.r2007.R2007SectionMap;
import io.dwg.core.util.ByteUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test reading SectionMap directly from header offset, bypassing PageMap.
 */
public class TestR2007DirectRead {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Testing Direct SectionMap Read (Bypass PageMap)");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        List<Path> r2007Files = Files.walk(Paths.get("./samples"))
            .filter(p -> p.toString().endsWith(".dwg"))
            .filter(p -> {
                try {
                    var v = DwgVersionDetector.detect(Files.readAllBytes(p));
                    return v.toString().contains("2007");
                } catch (Exception e) {
                    return false;
                }
            })
            .sorted()
            .limit(3)
            .collect(Collectors.toList());

        System.out.printf("Testing %d R2007 files\n\n", r2007Files.size());

        for (Path file : r2007Files) {
            try {
                byte[] data = Files.readAllBytes(file);
                System.out.printf("File: %s\n", file.getFileName());

                // Read offset from header[0x20]
                long offset = ByteUtils.readLE64(data, 0x20);
                System.out.printf("  Header offset[0x20]: 0x%X\n", offset);

                if (offset < 0 || offset >= data.length - 4) {
                    System.out.println("  ❌ Offset out of range\n");
                    continue;
                }

                // Read directly from offset as SectionMap
                int sectionCount = (int) ByteUtils.readLE32(data, (int) offset);
                System.out.printf("  Section count at offset: %d\n", sectionCount);

                if (sectionCount <= 0 || sectionCount > 20) {
                    System.out.println("  ❌ Invalid section count\n");
                    continue;
                }

                // Extract that region as a byte array and parse
                int dataSize = Math.min(65536, (int)(data.length - offset));
                byte[] sectionMapData = new byte[dataSize];
                System.arraycopy(data, (int)offset, sectionMapData, 0, dataSize);

                R2007SectionMap sectionMap = R2007SectionMap.read(sectionMapData);
                System.out.printf("  Parsed sections: %d\n", sectionMap.descriptors().size());

                if (sectionMap.descriptors().size() > 0) {
                    System.out.printf("  ✅ SUCCESS - First section: %s\n\n",
                        sectionMap.descriptors().get(0).name());
                } else {
                    System.out.println("  ❌ No sections parsed\n");
                }

            } catch (Exception e) {
                System.out.printf("  Error: %s\n\n", e.getMessage());
            }
        }

        System.out.println("═══════════════════════════════════════════════════════════════");
    }
}
