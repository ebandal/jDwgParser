package io.dwg.test;

import java.io.*;
import io.dwg.api.DwgReader;
import io.dwg.sections.handles.HandleRegistry;
import io.dwg.core.version.DwgVersion;

public class DebugHandleOffsets {
    public static void main(String[] args) throws Exception {
        File f = new File("samples/2007/Arc.dwg");
        
        System.out.println("=== R2007 Handle Offsets Analysis ===\n");
        System.out.println("File: Arc.dwg (" + f.length() + " bytes)\n");
        
        byte[] data = new byte[(int)f.length()];
        try (FileInputStream fis = new FileInputStream(f)) {
            fis.read(data);
        }
        
        DwgVersion version = io.dwg.core.version.DwgVersionDetector.detect(data);
        System.out.println("Detected version: " + version);
        
        // Read with DwgReader to populate handle registry
        io.dwg.api.DwgDocument doc = DwgReader.defaultReader().open(f.toPath());
        
        System.out.println("\nObject map size: " + doc.objectMap().size());
        System.out.println("Handle registry size: " + doc.handleRegistry().allHandles().size());

        // Get handle entries (offsets)
        var entries = doc.handleRegistry().sortedEntries();
        System.out.println("\nFirst 10 handle offsets:");

        int count = 0;
        for (var entry : entries) {
            if (count >= 10) break;
            long handle = entry.handle();
            long offset = entry.offset();
            System.out.printf("  Handle 0x%X: offset %,d (0x%X)%s\n",
                handle, offset, offset,
                (offset < 0 || offset >= 197318) ? " [OUT OF RANGE]" : "");
            count++;
        }

        // Statistics
        long minOffset = entries.stream().mapToLong(e -> e.offset()).min().orElse(0);
        long maxOffset = entries.stream().mapToLong(e -> e.offset()).max().orElse(0);
        long negCount = entries.stream().filter(e -> e.offset() < 0).count();
        
        System.out.println("\nOffsets statistics:");
        System.out.printf("  Min: %,d, Max: %,d\n", minOffset, maxOffset);
        System.out.printf("  Objects section size: 197,318 bytes\n");
        System.out.printf("  Negative offsets: %d\n", negCount);
        System.out.printf("  Out of range (>197318): %d\n",
            entries.stream().filter(e -> e.offset() > 197318).count());
    }
}
