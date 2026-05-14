package debug;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import java.nio.file.Paths;

/**
 * Test new R2000 implementation with Arc.dwg
 */
public class TestR2000Implementation {
    public static void main(String[] args) throws Exception {
        System.out.println("=== R2000 Implementation Test ===\n");

        String[] files = {
            "samples/2000/Arc.dwg",
            "samples/2000/circle.dwg",
            "samples/2000/Cone.dwg"
        };

        for (String filePath : files) {
            System.out.printf("Testing: %s\n", filePath);
            try {
                DwgDocument doc = DwgReader.defaultReader().open(Paths.get(filePath));

                System.out.printf("  Version: %s\n", doc.version());
                System.out.printf("  Objects: %d\n", doc.objectMap().size());
                System.out.printf("  Entities: %d\n", doc.entities().size());

                if (doc.objectMap().size() > 0) {
                    System.out.print("  Types: ");
                    doc.objectMap().values().stream()
                        .map(e -> e.getClass().getSimpleName())
                        .distinct()
                        .limit(5)
                        .forEach(t -> System.out.print(t + " "));
                    System.out.println();
                }

            } catch (Exception e) {
                System.out.printf("  ERROR: %s\n", e.getMessage());
                e.printStackTrace();
            }
            System.out.println();
        }
    }
}
