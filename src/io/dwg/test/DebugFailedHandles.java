package io.dwg.test;

import io.dwg.api.DwgReader;
import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.version.DwgVersion;
import io.dwg.core.version.DwgVersionDetector;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Debug failed handles - shows which handles fail to parse and why.
 */
public class DebugFailedHandles {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Debugging Failed Handles in Arc.dwg");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] data = Files.readAllBytes(Paths.get("./samples/2000/Arc.dwg"));
        DwgVersion version = DwgVersionDetector.detect(data);

        // Parse to get handle registry
        var doc = DwgReader.defaultReader().open(data);

        System.out.printf("Total handles in registry: %d\n", doc.handleRegistry().size());
        System.out.printf("Objects successfully parsed: %d\n", doc.objectMap().size());
        System.out.printf("Failed: %d\n\n", doc.handleRegistry().size() - doc.objectMap().size());

        System.out.println("First 20 failed handles:");
        System.out.println("───────────────────────────────────────────────────────────────");
        System.out.printf("%-10s %-10s %-10s %-15s\n", "Handle", "Offset", "ObjSize", "TypeCode");
        System.out.println("───────────────────────────────────────────────────────────────");

        int shown = 0;
        for (long handle : doc.handleRegistry().allHandles()) {
            if (doc.objectMap().containsKey(handle)) {
                continue; // Skip successfully parsed objects
            }

            if (shown >= 20) break;
            shown++;

            // Try to read the object header at this handle's offset
            try {
                var offsetOpt = doc.handleRegistry().offsetFor(handle);
                if (offsetOpt.isEmpty()) continue;

                int offset = (int)offsetOpt.get().longValue();
                ByteBufferBitInput buf = new ByteBufferBitInput(data);
                buf.seek((long) offset * 8L);
                BitStreamReader r = new BitStreamReader(buf, version);

                int objSize = r.readModularShort();
                int typeCode = r.readBitShort();
                String typeStr = String.format("0x%02X", typeCode);

                System.out.printf("0x%-8X 0x%-8X %-10d %s\n", handle, offset, objSize, typeStr);
            } catch (Exception e) {
                System.out.printf("0x%-8X ERROR: %s\n", handle, e.getMessage());
            }
        }

        System.out.println("═══════════════════════════════════════════════════════════════");
    }
}
