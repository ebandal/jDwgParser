package test.java.io.dwg.api;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.core.version.DwgVersion;
import java.nio.file.Paths;

/**
 * Test DwgReader with multiple DWG versions
 */
public class DwgReaderMultiVersionTest {
    public static void main(String[] args) throws Exception {
        System.out.println("=".repeat(80));
        System.out.println("DwgReader Multi-Version Test");
        System.out.println("=".repeat(80));
        System.out.println();

        String[] testFiles = {
            "samples/example_r13.dwg",
            "samples/example_2000.dwg",
            "samples/example_2004.dwg",
            "samples/example_2007.dwg",
            "samples/example_2010.dwg",
            "samples/example_2013.dwg",
            "samples/example_2018.dwg"
        };

        for (String filePath : testFiles) {
            try {
                System.out.printf("Testing: %s\n", filePath);
                DwgDocument doc = DwgReader.defaultReader().open(Paths.get(filePath));

                System.out.printf("  ✓ Version: %s\n", doc.version());
                System.out.printf("  ✓ ObjectMap size: %d\n", doc.objectMap().size());
                System.out.printf("  ✓ Entities count: %d\n", doc.entities().size());

                if (doc.objectMap().size() > 0) {
                    System.out.print("  ✓ Entity types: ");
                    doc.entities().stream()
                        .map(e -> e.getClass().getSimpleName())
                        .distinct()
                        .limit(5)
                        .forEach(t -> System.out.print(t + ", "));
                    System.out.println();
                }

            } catch (Exception e) {
                System.out.printf("  ✗ Error: %s\n", e.getMessage());
            }
            System.out.println();
        }

        System.out.println("=".repeat(80));
    }
}
