package io.dwg.test;

import io.dwg.api.DwgReader;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Test to verify marker types and ALTERNATE types are now parsing correctly.
 */
public class TestMarkerTypesFix {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Testing Marker Types & ALTERNATE Types Fix (Arc.dwg)");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] data = Files.readAllBytes(Paths.get("./samples/2000/Arc.dwg"));
        var doc = DwgReader.defaultReader().open(data);

        int totalHandles = doc.handleRegistry().size();
        int parsedObjects = doc.objectMap().size();
        int failedObjects = totalHandles - parsedObjects;

        System.out.printf("Total handles: %d\n", totalHandles);
        System.out.printf("Successfully parsed: %d\n", parsedObjects);
        System.out.printf("Failed: %d\n", failedObjects);
        System.out.printf("Success rate: %.1f%%\n\n", (100.0 * parsedObjects / totalHandles));

        // Expected improvement: 102 -> ~114 (adding SEQEND, BLOCK_END, APPID_ALTERNATE, MLINESTYLE_ALTERNATE, STYLE_ALTERNATE, VBA_PROJECT)
        if (parsedObjects > 102) {
            System.out.println("✓ IMPROVEMENT DETECTED! Objects increased from baseline 102");
        } else if (parsedObjects == 102) {
            System.out.println("⚠ No change from baseline (102) - may need further investigation");
        } else {
            System.out.println("✗ REGRESSION - object count decreased!");
        }

        System.out.println("═══════════════════════════════════════════════════════════════");
    }
}
