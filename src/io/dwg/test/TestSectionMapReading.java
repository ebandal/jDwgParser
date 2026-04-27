package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.format.r2007.R2007FileHeader;
import io.dwg.format.r2007.R2007SectionMapReader;
import io.dwg.format.r2007.R2007SectionMapParser;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Test SectionMap reading and parsing
 */
public class TestSectionMapReading {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Test: SectionMap Reading (same pipeline as PageMap)");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] fileData = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));
        ByteBuffer buf = ByteBuffer.wrap(fileData);
        BitInput input = new ByteBufferBitInput(buf);

        R2007FileHeader header = R2007FileHeader.read(input);

        System.out.printf("SectionMap Parameters:\n");
        System.out.printf("  sectionMapId:         0x%X\n", header.sectionsMapId());
        System.out.printf("  sectionMapSizeComp:   %d bytes\n", header.sectionsMapSizeComp());
        System.out.printf("  sectionMapSizeUncomp: %d bytes\n", header.sectionsMapSizeUncomp());
        System.out.printf("  pageMapCorrection:    %d (used as repeatCount)\n\n", header.pageMapCorrection());

        long sizeComp = header.sectionsMapSizeComp();
        long sizeUncomp = header.sectionsMapSizeUncomp();
        long repeatCount = header.pageMapCorrection();
        long sectionMapFileOffset = 0x480L + header.sectionsMapId();

        System.out.printf("Reading SectionMap from offset 0x%X...\n\n", sectionMapFileOffset);

        try {
            byte[] decompressed = R2007SectionMapReader.readSectionMap(
                input,
                sectionMapFileOffset,
                sizeComp,
                sizeUncomp,
                repeatCount
            );

            System.out.printf("✅ SectionMap decompressed successfully! (%d bytes)\n\n", decompressed.length);

            // Parse SectionMap
            List<R2007SectionMapParser.SectionMapEntry> sections =
                R2007SectionMapParser.parseSectionMap(decompressed);

            System.out.println("SectionMap Entries:");
            for (int i = 0; i < Math.min(sections.size(), 15); i++) {
                R2007SectionMapParser.SectionMapEntry entry = sections.get(i);
                System.out.printf("  [%2d] %s\n", i, entry);
            }
            if (sections.size() > 15) {
                System.out.printf("  ... and %d more entries\n", sections.size() - 15);
            }
            System.out.printf("\nTotal sections: %d\n", sections.size());

        } catch (Exception e) {
            System.out.printf("❌ Error: %s\n", e.getMessage());
            e.printStackTrace();
        }
    }
}
