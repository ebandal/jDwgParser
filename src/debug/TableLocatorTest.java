package debug;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.api.DwgTableLocator;
import io.dwg.entities.concrete.DwgLayer;
import io.dwg.entities.concrete.DwgLtype;
import io.dwg.entities.concrete.DwgStyle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * Phase 5: Table Object Locator Integration Test
 *
 * Tests the new DwgTableLocator API that provides structured access to table objects
 * (layers, linetypes, styles) that are already parsed by ObjectsSectionParser.
 */
public class TableLocatorTest {
    public static void main(String[] args) throws Exception {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║      PHASE 5: TABLE LOCATOR INTEGRATION TEST               ║");
        System.out.println("║  Verify DwgTableLocator provides clean access to tables    ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");

        // Find a test DWG file
        Path testFile = findTestFile();
        if (testFile == null) {
            System.out.println("❌ No DWG test files found in samples/ directory");
            return;
        }

        System.out.printf("🔍 Testing with file: %s\n\n", testFile.getFileName());

        try {
            // Parse the DWG file
            DwgDocument doc = DwgReader.defaultReader().open(testFile);
            System.out.printf("✓ File parsed successfully. DWG version: %s\n\n", doc.version());

            // Test the table locator API
            testTableLocator(doc);

            System.out.println("\n✅ All tests passed!");

        } catch (Exception e) {
            System.out.printf("❌ Test failed: %s\n", e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testTableLocator(DwgDocument doc) {
        DwgTableLocator locator = doc.tables();

        System.out.println("【 TEST 1: Layer Access 】\n");
        testLayers(locator);

        System.out.println("\n【 TEST 2: Linetype Access 】\n");
        testLinetypes(locator);

        System.out.println("\n【 TEST 3: Style Access 】\n");
        testStyles(locator);

        System.out.println("\n【 TEST 4: Named Lookups 】\n");
        testNamedLookups(locator);

        System.out.println("\n【 TEST 5: Direct DwgDocument Methods 】\n");
        testDirectMethods(doc);
    }

    private static void testLayers(DwgTableLocator locator) {
        List<DwgLayer> layers = locator.layers();
        System.out.printf("Found %d layers:\n", layers.size());

        if (layers.isEmpty()) {
            System.out.println("⚠️  WARNING: No layers found!");
        } else {
            for (int i = 0; i < Math.min(5, layers.size()); i++) {
                DwgLayer layer = layers.get(i);
                System.out.printf("  • %s (frozen=%b, on=%b)\n",
                    layer.name(), layer.isFrozen(), layer.isOn());
            }
            if (layers.size() > 5) {
                System.out.printf("  ... and %d more\n", layers.size() - 5);
            }
        }

        // Verify the default "0" layer exists
        boolean hasDefaultLayer = layers.stream().anyMatch(l -> l.name().equals("0"));
        if (hasDefaultLayer) {
            System.out.println("✅ Default layer \"0\" found");
        } else {
            System.out.println("⚠️  Default layer \"0\" not found (unusual but not critical)");
        }
    }

    private static void testLinetypes(DwgTableLocator locator) {
        List<DwgLtype> linetypes = locator.linetypes();
        System.out.printf("Found %d linetypes:\n", linetypes.size());

        if (linetypes.isEmpty()) {
            System.out.println("⚠️  WARNING: No linetypes found!");
        } else {
            for (int i = 0; i < Math.min(5, linetypes.size()); i++) {
                DwgLtype lt = linetypes.get(i);
                System.out.printf("  • %s (length=%.2f, dashes=%d)\n",
                    lt.name(), lt.totalLength(), lt.numDashes());
            }
            if (linetypes.size() > 5) {
                System.out.printf("  ... and %d more\n", linetypes.size() - 5);
            }
        }

        // Verify the default "Continuous" linetype exists
        boolean hasContinuous = linetypes.stream().anyMatch(lt -> lt.name().equals("Continuous"));
        if (hasContinuous) {
            System.out.println("✅ Default linetype \"Continuous\" found");
        } else {
            System.out.println("⚠️  Default linetype \"Continuous\" not found (unusual)");
        }
    }

    private static void testStyles(DwgTableLocator locator) {
        List<DwgStyle> styles = locator.styles();
        System.out.printf("Found %d text styles:\n", styles.size());

        if (styles.isEmpty()) {
            System.out.println("⚠️  WARNING: No text styles found!");
        } else {
            for (int i = 0; i < Math.min(5, styles.size()); i++) {
                DwgStyle style = styles.get(i);
                System.out.printf("  • %s (font=%s, width=%.2f)\n",
                    style.name(), style.fontFilename(), style.width());
            }
            if (styles.size() > 5) {
                System.out.printf("  ... and %d more\n", styles.size() - 5);
            }
        }

        // Verify the default "Standard" style exists
        boolean hasStandard = styles.stream().anyMatch(s -> s.name().equals("Standard"));
        if (hasStandard) {
            System.out.println("✅ Default style \"Standard\" found");
        } else {
            System.out.println("⚠️  Default style \"Standard\" not found (unusual)");
        }
    }

    private static void testNamedLookups(DwgTableLocator locator) {
        // Test layer lookup
        Optional<DwgLayer> layer0 = locator.layerByName("0");
        if (layer0.isPresent()) {
            System.out.println("✅ Layer lookup by name works: found \"0\"");
        } else {
            System.out.println("⚠️  Could not find layer \"0\" by name");
        }

        // Test linetype lookup
        Optional<DwgLtype> continuous = locator.linetypeByName("Continuous");
        if (continuous.isPresent()) {
            System.out.println("✅ Linetype lookup by name works: found \"Continuous\"");
        } else {
            System.out.println("⚠️  Could not find linetype \"Continuous\" by name");
        }

        // Test style lookup
        Optional<DwgStyle> standard = locator.styleByName("Standard");
        if (standard.isPresent()) {
            System.out.println("✅ Style lookup by name works: found \"Standard\"");
        } else {
            System.out.println("⚠️  Could not find style \"Standard\" by name");
        }
    }

    private static void testDirectMethods(DwgDocument doc) {
        System.out.println("Verifying DwgDocument convenience methods:");

        // Test direct methods on DwgDocument
        List<DwgLayer> layers = doc.layers();
        List<DwgLtype> linetypes = doc.linetypes();
        List<DwgStyle> styles = doc.styles();

        System.out.printf("  ✅ doc.layers() returns %d layers\n", layers.size());
        System.out.printf("  ✅ doc.linetypes() returns %d linetypes\n", linetypes.size());
        System.out.printf("  ✅ doc.styles() returns %d styles\n", styles.size());

        // Test named lookups on DwgDocument
        Optional<DwgLayer> layer = doc.layer("0");
        Optional<DwgLtype> linetype = doc.linetype("Continuous");
        Optional<DwgStyle> style = doc.style("Standard");

        if (layer.isPresent()) System.out.println("  ✅ doc.layer(name) works");
        if (linetype.isPresent()) System.out.println("  ✅ doc.linetype(name) works");
        if (style.isPresent()) System.out.println("  ✅ doc.style(name) works");
    }

    private static Path findTestFile() throws Exception {
        // Search for a DWG file in samples/ directory
        Path samplesDir = Paths.get("samples");
        if (!Files.exists(samplesDir)) {
            System.out.println("⚠️  samples/ directory not found");
            return null;
        }

        // Try to find a working sample file (prefer R2004+)
        return Files.walk(samplesDir)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".dwg"))
            .sorted()
            .findFirst()
            .orElse(null);
    }
}
