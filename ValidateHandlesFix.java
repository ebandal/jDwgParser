import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import java.io.File;

/**
 * Validate the R2007 Handles blockCount fix
 * Tests all 10 R2007 sample files for entity parsing
 */
public class ValidateHandlesFix {
    public static void main(String[] args) throws Exception {
        String[] testFiles = {
            "Arc", "Constraints", "ConstructionLine", "Donut", "Ellipse",
            "Leader", "Multiline", "Point", "RAY", "Spline"
        };

        System.out.println("=== R2007 Handles Fix Validation ===\n");
        System.out.println(String.format("%-20s %8s %7s %8s %9s",
            "File", "Entities", "Layers", "Linetypes", "Status"));
        System.out.println(new String(new char[60]).replace('\0', '-'));

        int totalEntities = 0;
        int filesWithEntities = 0;
        int totalFiles = 0;

        for (String name : testFiles) {
            File f = new File("samples/2007/" + name + ".dwg");
            if (!f.exists()) {
                System.out.printf("%-20s FILE NOT FOUND\n", name + ".dwg");
                continue;
            }

            try {
                totalFiles++;
                DwgDocument doc = DwgReader.defaultReader().open(f.toPath());
                int entityCount = doc.entities().size();
                int layerCount = doc.layers().size();
                int linetypeCount = doc.linetypes().size();

                String status = entityCount > 0 ? "✓ OK" : "✗ 0 entities";
                System.out.printf("%-20s %8d %7d %8d %9s\n",
                    name + ".dwg", entityCount, layerCount, linetypeCount, status);

                totalEntities += entityCount;
                if (entityCount > 0) filesWithEntities++;

            } catch (Exception e) {
                System.out.printf("%-20s ERROR: %s\n", name + ".dwg",
                    e.getClass().getSimpleName());
            }
        }

        System.out.println(new String(new char[60]).replace('\0', '-'));
        System.out.printf("\nFiles with entities: %d/%d\n", filesWithEntities, totalFiles);
        System.out.printf("Total entities: %d\n", totalEntities);

        if (filesWithEntities > 1) {
            System.out.println("\n✓ SUCCESS: Handles fix appears to be working!");
            System.out.println("  Multiple files now have parsed entities.");
        } else if (totalEntities > 4) {
            System.out.println("\n✓ PARTIAL: More entities than before (4 baseline)");
        } else {
            System.out.println("\n✗ Fix may not have worked - still 0-4 entities");
        }
    }
}
