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
 * Test R2000 Objects parsing with new binary format support
 */
public class TestR2000ObjectsParsing {
    public static void main(String[] args) throws Exception {
        Path testFile = Paths.get("C:\\workspace_ebandal\\jDwgParser\\samples\\2000\\Arc.dwg");

        if (!Files.exists(testFile)) {
            System.err.println("Test file not found: " + testFile);
            return;
        }

        byte[] fileData = Files.readAllBytes(testFile);
        ByteBufferBitInput input = new ByteBufferBitInput(fileData);

        System.out.println("=== R2000 Objects Parsing Test (Arc.dwg) ===\n");

        // Parse header
        R2000FileStructureHandler handler = new R2000FileStructureHandler();
        FileHeaderFields header = handler.readHeader(input);

        // Get sections
        var sections = handler.readSections(input, header);

        // Parse Objects section
        var objectsSection = sections.get(io.dwg.format.common.SectionType.OBJECTS.sectionName());
        if (objectsSection == null) {
            System.err.println("ERROR: Objects section not found!");
            return;
        }

        System.out.printf("Objects section size: %d bytes\n\n", objectsSection.rawBytes().length);

        // Parse Objects
        ObjectsSectionParser parser = new ObjectsSectionParser();
        var objects = parser.parse(objectsSection, DwgVersion.R2000);

        System.out.printf("\n=== Results ===\n");
        System.out.printf("Total objects parsed: %d\n", objects.size());

        // Show first few objects
        int count = 0;
        for (var entry : objects.entrySet()) {
            if (count++ >= 10) break;
            System.out.printf("  Handle 0x%X: %s\n", entry.getKey(), entry.getValue().getClass().getSimpleName());
        }
    }
}
