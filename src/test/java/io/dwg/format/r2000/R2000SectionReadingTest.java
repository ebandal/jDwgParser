package test.java.io.dwg.format.r2000;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;

import java.nio.file.Paths;

/**
 * Test whether R2000 file handler can now properly read section locators
 */
public class R2000SectionReadingTest {
    public static void main(String[] args) throws Exception {
        System.out.println("=".repeat(80));
        System.out.println("R2000 Section Reading Test - After Fix");
        System.out.println("=".repeat(80));
        System.out.println();

        String filePath = "samples/example_2000.dwg";

        try {
            DwgDocument doc = DwgReader.defaultReader().open(Paths.get(filePath));

            System.out.println("✓ File parsed successfully");
            System.out.println("  Version: " + doc.version());
            System.out.println();

            System.out.println("ObjectMap contents:");
            System.out.println("  Size: " + doc.objectMap().size());

            if (doc.objectMap().size() > 0) {
                System.out.println("  ✓ SUCCESS! ObjectMap is now populated!");
                System.out.println("  First few entries:");
                doc.objectMap().entrySet().stream()
                    .limit(5)
                    .forEach(e -> System.out.println("    - " + e.getKey() + ": " + e.getValue()));
            } else {
                System.out.println("  ✗ FAILED: ObjectMap is still empty");
            }

            System.out.println();
            System.out.println("Entities count: " + doc.entities().size());
            if (doc.entities().size() > 0) {
                System.out.println("  ✓ SUCCESS! Entities found!");
                System.out.println("  First few entities:");
                doc.entities().stream()
                    .limit(5)
                    .forEach(e -> System.out.println("    - " + e.getClass().getSimpleName()));
            } else {
                System.out.println("  ✗ No entities found yet");
            }

            System.out.println();
            System.out.println("=".repeat(80));

        } catch (Exception e) {
            System.err.println("✗ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
