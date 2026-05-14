package io.dwg.test;

import io.dwg.api.DwgReader;
import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.version.DwgVersion;
import io.dwg.core.version.DwgVersionDetector;
import io.dwg.entities.DwgObjectType;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Analyze BadObjSize failures to understand patterns.
 */
public class AnalyzeBadObjSize {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Analyzing BadObjSize Failures in Arc.dwg");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] data = Files.readAllBytes(Paths.get("./samples/2000/Arc.dwg"));
        DwgVersion version = DwgVersionDetector.detect(data);

        var doc = DwgReader.defaultReader().open(data);

        System.out.printf("Total handles: %d\n", doc.handleRegistry().size());
        System.out.printf("Successfully parsed: %d\n", doc.objectMap().size());
        System.out.printf("Failed: %d\n\n", doc.handleRegistry().size() - doc.objectMap().size());

        // Analyze BadObjSize failures
        Map<String, Integer> sizePatterns = new TreeMap<>();
        Map<String, Integer> typePatterns = new TreeMap<>();
        int badSizeCount = 0;
        int zeroSizeCount = 0;
        int negativeSizeCount = 0;

        for (long handle : doc.handleRegistry().allHandles()) {
            if (doc.objectMap().containsKey(handle)) {
                continue;
            }

            var offsetOpt = doc.handleRegistry().offsetFor(handle);
            if (offsetOpt.isEmpty()) continue;

            int offset = (int)offsetOpt.get().longValue();

            try {
                ByteBufferBitInput buf = new ByteBufferBitInput(data);
                buf.seek((long) offset * 8L);
                BitStreamReader r = new BitStreamReader(buf, version);

                int objSize = r.readModularShort();
                int typeCode = r.readBitShort();

                if (objSize <= 0) {
                    badSizeCount++;
                    if (objSize == 0) zeroSizeCount++;
                    else negativeSizeCount++;

                    String typeName = DwgObjectType.fromCode(typeCode).toString();
                    String sizeKey = String.format("%s (size=%d)", typeName, objSize);
                    sizePatterns.merge(sizeKey, 1, Integer::sum);
                    typePatterns.merge(typeName, 1, Integer::sum);

                    if (badSizeCount <= 20) {
                        System.out.printf("  Handle 0x%X: type=%s size=%d\n", handle, typeName, objSize);
                    }
                }
            } catch (Exception e) {
                // Skip
            }
        }

        System.out.println("\n───────────────────────────────────────────────────────────────");
        System.out.println("BadObjSize Summary:");
        System.out.printf("  Total with size <= 0: %d\n", badSizeCount);
        System.out.printf("  Zero size: %d\n", zeroSizeCount);
        System.out.printf("  Negative size: %d\n", negativeSizeCount);

        System.out.println("\n───────────────────────────────────────────────────────────────");
        System.out.println("BadObjSize by Type:");
        typePatterns.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .forEach(e -> System.out.printf("  %s: %d\n", e.getKey(), e.getValue()));

        System.out.println("\n───────────────────────────────────────────────────────────────");
        System.out.println("Detailed Size Patterns (first 25):");
        sizePatterns.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(25)
            .forEach(e -> System.out.printf("  %s: %d\n", e.getKey(), e.getValue()));

        System.out.println("═══════════════════════════════════════════════════════════════");
    }
}
