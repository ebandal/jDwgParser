package io.dwg.test;

import java.io.*;
import io.dwg.api.DwgReader;
import io.dwg.sections.handles.HandleRegistry;

public class AnalyzeHandlesData {
    public static void main(String[] args) throws Exception {
        String[] files = {"Constraints", "Arc"};
        
        for (String name : files) {
            File f = new File("samples/2007/" + name + ".dwg");
            if (!f.exists()) continue;
            
            System.out.println("\n=== " + name + ".dwg Handles Analysis ===");
            DwgReader reader = DwgReader.defaultReader();
            var doc = reader.open(f.toPath());
            var registry = doc.handleRegistry();
            
            var entries = registry.sortedEntries();
            System.out.println("Total handles: " + entries.size());
            System.out.println("\nFirst 10 handles:");
            
            int count = 0;
            for (var entry : entries) {
                if (count >= 10) break;
                long handle = entry.handle();
                long offset = entry.offset();
                boolean valid = offset >= 0 && offset < 300000;
                System.out.printf("  Handle 0x%X: offset %,d (0x%X) %s\n",
                    handle, offset, offset, valid ? "✓" : "✗ INVALID");
                count++;
            }
            
            System.out.println("\nOffset statistics:");
            long negCount = entries.stream().filter(e -> e.offset() < 0).count();
            long outOfRangeCount = entries.stream().filter(e -> e.offset() >= 300000).count();
            long minOffset = entries.stream().mapToLong(e -> e.offset()).min().orElse(0);
            long maxOffset = entries.stream().mapToLong(e -> e.offset()).max().orElse(0);
            
            System.out.printf("  Negative: %d, Out-of-range: %d\n", negCount, outOfRangeCount);
            System.out.printf("  Min: %,d, Max: %,d\n", minOffset, maxOffset);
        }
    }
}
