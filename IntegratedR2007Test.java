import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import java.io.File;

/**
 * Integrated test for R2007 Handles blockCount fix
 * Tests complete pipeline: Extraction → Handles parsing → Entity parsing
 */
public class IntegratedR2007Test {
    public static void main(String[] args) throws Exception {
        String[] files = {
            "samples/2007/Arc.dwg",
            "samples/2007/Constraints.dwg",
            "samples/2007/ConstructionLine.dwg",
            "samples/2007/Donut.dwg",
            "samples/2007/Ellipse.dwg",
            "samples/2007/Leader.dwg",
            "samples/2007/Multiline.dwg",
            "samples/2007/Point.dwg",
            "samples/2007/RAY.dwg",
            "samples/2007/Spline.dwg"
        };

        System.out.println("=== INTEGRATED R2007 HANDLES FIX TEST ===\n");
        System.out.println(String.format("%-25s %8s %8s %7s %6s",
            "File", "Handles", "Entities", "Layers", "Pass"));
        System.out.println(new String(new char[70]).replace('\0', '-'));

        int totalFiles = 0;
        int successFiles = 0;
        int totalEntities = 0;
        int totalHandles = 0;

        for (String filePath : files) {
            File f = new File(filePath);
            String fileName = f.getName();

            try {
                totalFiles++;
                DwgDocument doc = DwgReader.defaultReader().open(f.toPath());

                int handles = doc.handleRegistry() != null ?
                    doc.handleRegistry().allHandles().size() : 0;
                int entities = doc.entities().size();
                int layers = doc.layers().size();

                totalHandles += handles;
                totalEntities += entities;

                // Success criteria:
                // 1. Has handles (Handles section extracted)
                // 2. Has entities (offset-based or sequential parsing worked)
                boolean success = handles > 0 && entities > 0;
                if (success) successFiles++;

                String passStr = success ? "✓" : (handles > 0 ? "⚠" : "✗");
                System.out.printf("%-25s %8d %8d %7d %6s\n",
                    fileName, handles, entities, layers, passStr);

            } catch (Exception e) {
                totalFiles++;
                System.out.printf("%-25s ERROR: %s\n", fileName, e.getMessage());
            }
        }

        System.out.println(new String(new char[70]).replace('\0', '-'));
        System.out.printf("Summary: %d/%d files, %d handles, %d entities\n",
            successFiles, totalFiles, totalHandles, totalEntities);
        System.out.printf("Success rate: %.0f%%\n", 100.0 * successFiles / totalFiles);

        // Validation thresholds
        System.out.println("\nValidation Results:");
        if (successFiles >= 8) {
            System.out.println("✅ PASS: 8+ files with both handles and entities");
        } else if (successFiles >= 5) {
            System.out.println("⚠️  PARTIAL: 5-7 files with both handles and entities");
        } else {
            System.out.println("❌ FAIL: <5 files with both handles and entities");
        }

        if (totalEntities >= 500) {
            System.out.println("✅ PASS: 500+ total entities (vs 4 baseline)");
        } else if (totalEntities >= 100) {
            System.out.println("⚠️  PARTIAL: 100-500 entities");
        } else {
            System.out.println("❌ FAIL: <100 entities");
        }

        if (totalHandles >= 2000) {
            System.out.println("✅ PASS: 2000+ total handles extracted");
        }
    }
}
