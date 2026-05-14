package debug;

import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.r2000.R2000FileStructureHandler;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.sections.objects.ObjectsSectionParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test R2000 Objects parsing on all R2000 sample files
 */
public class TestR2000AllSamples {
    public static void main(String[] args) throws Exception {
        Path sampleDir = Paths.get("C:\\workspace_ebandal\\jDwgParser\\samples\\2000");

        if (!Files.exists(sampleDir)) {
            System.err.println("Sample directory not found: " + sampleDir);
            return;
        }

        System.out.println("=== Testing R2000 Objects Parsing ===\n");

        int totalFiles = 0;
        int totalObjects = 0;

        // Test each R2000 file
        var dwgFiles = Files.list(sampleDir)
            .filter(p -> p.toString().endsWith(".dwg"))
            .sorted()
            .toList();

        for (Path testFile : dwgFiles) {
            totalFiles++;
            try {
                byte[] fileData = Files.readAllBytes(testFile);
                ByteBufferBitInput input = new ByteBufferBitInput(fileData);

                R2000FileStructureHandler handler = new R2000FileStructureHandler();
                FileHeaderFields header = handler.readHeader(input);
                var sections = handler.readSections(input, header);

                var objectsSection = sections.get(io.dwg.format.common.SectionType.OBJECTS.sectionName());
                if (objectsSection == null) {
                    System.out.printf("❌ %s: NO OBJECTS SECTION\n", testFile.getFileName());
                    continue;
                }

                ObjectsSectionParser parser = new ObjectsSectionParser();
                var objects = parser.parse(objectsSection, DwgVersion.R2000);

                totalObjects += objects.size();

                System.out.printf("✓ %s: %d objects\n", testFile.getFileName(), objects.size());

            } catch (Exception e) {
                System.out.printf("❌ %s: ERROR - %s\n", testFile.getFileName(), e.getMessage());
            }
        }

        System.out.printf("\n=== Summary ===\n");
        System.out.printf("Files tested: %d\n", totalFiles);
        System.out.printf("Total objects parsed: %d\n", totalObjects);
        System.out.printf("Average objects per file: %.1f\n", (double) totalObjects / totalFiles);
    }
}
