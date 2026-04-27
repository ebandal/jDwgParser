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
 * Analyze successfully parsed UNKNOWN types to understand patterns.
 */
public class AnalyzeUnknownPatterns {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Analyzing UNKNOWN Type Patterns (Successfully Parsed)");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] data = Files.readAllBytes(Paths.get("./samples/2000/Arc.dwg"));
        DwgVersion version = DwgVersionDetector.detect(data);

        var doc = DwgReader.defaultReader().open(data);

        Map<String, Integer> unknownTypes = new TreeMap<>();
        int totalUnknown = 0;
        int unknownSuccess = 0;

        for (long handle : doc.handleRegistry().allHandles()) {
            var offsetOpt = doc.handleRegistry().offsetFor(handle);
            if (offsetOpt.isEmpty()) continue;

            int offset = (int)offsetOpt.get().longValue();

            try {
                ByteBufferBitInput buf = new ByteBufferBitInput(data);
                buf.seek((long) offset * 8L);
                BitStreamReader r = new BitStreamReader(buf, version);

                int objSize = r.readModularShort();
                int typeCode = r.readBitShort();

                if (objSize > 0 && objSize <= 100000) {
                    DwgObjectType type = DwgObjectType.fromCode(typeCode);
                    if (type == DwgObjectType.UNKNOWN) {
                        totalUnknown++;
                        String typeKey = String.format("0x%04X", typeCode);
                        unknownTypes.merge(typeKey, 1, Integer::sum);

                        if (doc.objectMap().containsKey(handle)) {
                            unknownSuccess++;
                        }
                    }
                }
            } catch (Exception e) {
                // Skip
            }
        }

        System.out.printf("Total UNKNOWN types found: %d\n", totalUnknown);
        System.out.printf("Successfully parsed: %d\n", unknownSuccess);
        System.out.printf("Failed: %d\n\n", totalUnknown - unknownSuccess);

        System.out.println("UNKNOWN Type Codes by Frequency:");
        unknownTypes.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .forEach(e -> System.out.printf("  %s: %d\n", e.getKey(), e.getValue()));

        System.out.println("\n═══════════════════════════════════════════════════════════════");
    }
}
