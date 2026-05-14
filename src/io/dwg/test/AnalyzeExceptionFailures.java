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
 * Analyze exception failures during object parsing.
 * Shows which type codes cause bit stream reading errors.
 */
public class AnalyzeExceptionFailures {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Analyzing Exception Failures in Arc.dwg");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] data = Files.readAllBytes(Paths.get("./samples/2000/Arc.dwg"));
        DwgVersion version = DwgVersionDetector.detect(data);

        var doc = DwgReader.defaultReader().open(data);

        System.out.printf("Total handles: %d\n", doc.handleRegistry().size());
        System.out.printf("Successfully parsed: %d\n", doc.objectMap().size());
        System.out.printf("Failed: %d\n\n", doc.handleRegistry().size() - doc.objectMap().size());

        // Test exception failures
        Map<String, Integer> exceptionTypes = new TreeMap<>();
        int exceptionCount = 0;

        for (long handle : doc.handleRegistry().allHandles()) {
            if (doc.objectMap().containsKey(handle)) {
                continue; // Skip successful ones
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

                if (objSize > 0 && objSize <= 100000) {
                    // Valid size - try reading full object header
                    try {
                        long bitfield = r.readBitLong();
                        // If we got here, it's not an exception
                    } catch (Exception e) {
                        String typeName = DwgObjectType.fromCode(typeCode).toString();
                        String exceptionMsg = e.getClass().getSimpleName() + ": " + e.getMessage();
                        exceptionTypes.merge(typeName + " -> " + exceptionMsg, 1, Integer::sum);
                        exceptionCount++;
                    }
                }
            } catch (Exception e) {
                // Initial read failed - not detailed enough
            }
        }

        System.out.println("Exception Failure Types (first 15):");
        System.out.println("───────────────────────────────────────────────────────────────");
        exceptionTypes.entrySet().stream()
            .limit(15)
            .forEach(e -> System.out.printf("  %s: %d\n", e.getKey(), e.getValue()));

        System.out.printf("\nTotal exception failures analyzed: %d\n", exceptionCount);
        System.out.println("═══════════════════════════════════════════════════════════════");
    }
}
