package io.dwg.test;

import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.version.DwgVersionDetector;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.format.r2007.R2007FileStructureHandler;
import io.dwg.core.io.SectionInputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Test R2007 file structure fixes
 * - RS header decoding
 * - PageMap parsing (0x100 + offset)
 * - SectionMap parsing (8 RLL fields + UTF-16LE name + pages)
 */
public class TestR2007Fixes {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Testing R2007 Structure Fixes");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        Path dwgPath = Paths.get("./samples/2007/Arc.dwg");
        if (!dwgPath.toFile().exists()) {
            System.out.println("❌ Arc.dwg not found at " + dwgPath.toAbsolutePath());
            return;
        }

        try {
            byte[] data = Files.readAllBytes(dwgPath);

            // Step 1: Detect version
            DwgVersion version = DwgVersionDetector.detect(data);
            System.out.println("✓ Version detected: " + version);

            if (version != DwgVersion.R2007) {
                System.out.println("⚠️ Expected R2007 but got " + version);
            }

            // Step 2: Create BitInput and read header
            ByteBufferBitInput input = new ByteBufferBitInput(data);
            R2007FileStructureHandler handler = new R2007FileStructureHandler();
            FileHeaderFields header = handler.readHeader(input);

            System.out.println("\nHeader Fields:");
            System.out.printf("  pageMapOffset:          0x%X\n", header.pageMapOffset());
            System.out.printf("  pageMapSizeComp:        %d bytes\n", header.pageMapSizeComp());
            System.out.printf("  pageMapSizeUncomp:      %d bytes\n", header.pageMapSizeUncomp());
            System.out.printf("  sectionsMapId:          %d\n", header.sectionMapId());
            System.out.printf("  sectionsMapSizeComp:    %d bytes\n", header.sectionsMapSizeComp());
            System.out.printf("  sectionsMapSizeUncomp:  %d bytes\n\n", header.sectionsMapSizeUncomp());

            // Step 3: Reset input and read sections
            input = new ByteBufferBitInput(data);
            input.seek(0);
            Map<String, SectionInputStream> sections = handler.readSections(input, header);

            System.out.println("Sections parsed:");
            if (sections.isEmpty()) {
                System.out.println("  ❌ No sections parsed!");
            } else {
                for (String sectionName : sections.keySet()) {
                    SectionInputStream sis = sections.get(sectionName);
                    System.out.printf("  ✓ %s (%d bytes)\n", sectionName, sis.size());
                }
                System.out.println("\n✅ R2007 parsing completed successfully!");
                System.out.printf("   Found %d sections\n", sections.size());
            }

        } catch (Exception e) {
            System.out.println("\n❌ Error during parsing:");
            e.printStackTrace();
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
    }
}
