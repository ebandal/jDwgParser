package run;

import io.dwg.core.version.DwgVersionDetector;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.DwgFileStructureHandler;
import io.dwg.format.common.DwgFileStructureHandlerFactory;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.SectionInputStream;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Test R2004 section map parsing and section extraction
 */
public class R2004SectionMapTest {
    public static void main(String[] args) throws Exception {
        String[] testFiles = {
                "samples/example_2004.dwg",
        };

        for (String filePath : testFiles) {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("Testing: " + filePath);
            System.out.println("=".repeat(80));

            try {
                byte[] data = Files.readAllBytes(Paths.get(filePath));
                System.out.println("✓ File loaded: " + data.length + " bytes");

                // Detect version
                DwgVersion version = DwgVersionDetector.detect(data);
                System.out.println("✓ Version: " + version);

                // Get handler
                DwgFileStructureHandler handler =
                    DwgFileStructureHandlerFactory.forVersion(version);
                System.out.println("✓ Handler: " + handler.getClass().getSimpleName());

                // Read header
                BitInput input = new ByteBufferBitInput(data);
                FileHeaderFields headerFields = handler.readHeader(input);
                System.out.println("✓ Header read");

                // Read sections
                input = new ByteBufferBitInput(data);
                Map<String, SectionInputStream> sections =
                    handler.readSections(input, headerFields);

                System.out.println("\n✓ Sections extracted: " + sections.size());
                for (String name : sections.keySet()) {
                    SectionInputStream stream = sections.get(name);
                    System.out.println("  - " + name + " (" + stream.size() + " bytes)");
                }

                // Check for Objects section
                if (sections.containsKey("AcDb:AcDbObjects")) {
                    System.out.println("\n✅ SUCCESS: Found AcDb:AcDbObjects section!");
                } else {
                    System.out.println("\n❌ FAILED: AcDb:AcDbObjects section not found");
                    System.out.println("   Available: " + sections.keySet());
                }

            } catch (Exception e) {
                System.out.println("❌ ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
