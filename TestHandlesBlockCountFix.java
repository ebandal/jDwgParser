import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.BitInput;
import io.dwg.format.r2007.R2007FileStructureHandler;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.core.io.SectionInputStream;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Test R2007 Handles blockCount fix for Arc.dwg and other files
 */
public class TestHandlesBlockCountFix {
    public static void main(String[] args) throws Exception {
        String[] testFiles = {
            "samples/R2007/Arc.dwg",
            "samples/R2007/Constraints.dwg",
            "samples/R2007/ConstructionLine.dwg",
            "samples/R2007/Donut.dwg",
            "samples/R2007/Ellipse.dwg"
        };

        System.out.println("Testing R2007 Handles blockCount fix...\n");

        for (String filePath : testFiles) {
            try {
                System.out.println("Processing: " + filePath);
                byte[] fileData = Files.readAllBytes(Paths.get(filePath));
                BitInput input = new ByteBufferBitInput(java.nio.ByteBuffer.wrap(fileData));

                R2007FileStructureHandler handler = new R2007FileStructureHandler();
                FileHeaderFields header = handler.readHeader(input);

                // Read sections (which will trigger Handles extraction with our fix)
                input = new ByteBufferBitInput(java.nio.ByteBuffer.wrap(fileData));
                Map<String, SectionInputStream> sections = handler.readSections(input, header);

                // Check if Handles section was extracted
                if (sections.containsKey("AcDb:Handles")) {
                    SectionInputStream handlesSection = sections.get("AcDb:Handles");
                    System.out.printf("✓ Handles extracted: %d bytes\n", handlesSection.data().length);
                } else {
                    System.out.println("✗ Handles NOT extracted");
                }
                System.out.println();

            } catch (Exception e) {
                System.err.println("✗ Error: " + e.getMessage());
                e.printStackTrace();
                System.out.println();
            }
        }
    }
}
