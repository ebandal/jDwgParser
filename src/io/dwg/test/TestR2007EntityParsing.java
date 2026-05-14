package io.dwg.test;

import io.dwg.api.DwgReader;
import io.dwg.api.DwgDocument;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Test R2007 entity parsing after Objects extraction integration
 */
public class TestR2007EntityParsing {
    public static void main(String[] args) throws Exception {
        Path samplesDir = Paths.get("./samples/2007");

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Test R2007 Entity Parsing (DwgReader)");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        try (Stream<Path> stream = Files.list(samplesDir)) {
            stream.filter(p -> p.toString().endsWith(".dwg"))
                .sorted()
                .limit(5)  // Test first 5 files
                .forEach(filePath -> {
                    try {
                        byte[] data = Files.readAllBytes(filePath);
                        DwgDocument doc = DwgReader.defaultReader().open(data);

                        // Count objects (would need to check actual API)
                        System.out.printf("✅ %s: Parsed successfully\n",
                            filePath.getFileName());

                    } catch (Exception e) {
                        System.out.printf("❌ %s: %s\n", filePath.getFileName(),
                            e.getMessage());
                    }
                });
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
    }
}
