package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.format.r2007.R2007FileHeader;
import io.dwg.format.r2007.R2007SystemPageReader;
import io.dwg.format.r2007.R2007PageMapParser;
import io.dwg.format.r2007.R2007SectionMapReader;
import io.dwg.format.r2007.R2007SectionMapParser;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Test: Calculate file offset of SectionMap using PageMap
 * SectionMap = data section offset + cumulative size of all preceding pages
 */
public class TestSectionMapPageOffset {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Test: Calculate SectionMap offset from PageMap");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] fileData = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));
        ByteBuffer buf = ByteBuffer.wrap(fileData);
        BitInput input = new ByteBufferBitInput(buf);

        R2007FileHeader header = R2007FileHeader.read(input);

        int sectionMapPageId = (int)(header.sectionsMapId() & 0xFFFFFFFF);
        long dataSection = 0x480L + header.pageMapOffset();

        System.out.printf("Data section starts at: 0x%X\n", dataSection);
        System.out.printf("Looking for page 0x%X\n\n", sectionMapPageId);

        // Read PageMap
        long pageMapFileOffset = 0x480L + header.pageMapOffset();
        byte[] pageMapDecompressed = R2007SystemPageReader.readSystemPage(
            input,
            pageMapFileOffset,
            header.pageMapSizeComp(),
            header.pageMapSizeUncomp(),
            header.pageMapCorrection()
        );

        List<R2007PageMapParser.PageMapEntry> pageMap =
            R2007PageMapParser.parsePageMap(pageMapDecompressed);

        // Calculate cumulative offset
        long cumulativeOffset = 0;
        int pageIndex = 0;
        boolean found = false;

        System.out.println("Pages in order:");
        for (R2007PageMapParser.PageMapEntry entry : pageMap) {
            if (entry.pageId == sectionMapPageId) {
                found = true;
                System.out.printf("+%05X [%02d] Page 0x%02X: size=0x%04X ← SECTION MAP\n",
                    cumulativeOffset, pageIndex, entry.pageId, entry.size);
                break;
            } else {
                System.out.printf("+%05X [%02d] Page 0x%02X: size=0x%04X\n",
                    cumulativeOffset, pageIndex, entry.pageId, entry.size);
                cumulativeOffset += entry.size;
                pageIndex++;
            }
        }

        if (!found) {
            System.out.println("❌ SectionMap page not found!");
            return;
        }

        long sectionMapFileOffset = dataSection + cumulativeOffset;
        System.out.printf("\n✅ SectionMap file offset: 0x%X\n\n", sectionMapFileOffset);

        // Try reading SectionMap from calculated offset
        System.out.printf("Attempting to read SectionMap from 0x%X...\n", sectionMapFileOffset);

        try {
            byte[] decompressed = R2007SectionMapReader.readSectionMap(
                input,
                sectionMapFileOffset,
                header.sectionsMapSizeComp(),
                header.sectionsMapSizeUncomp(),
                header.pageMapCorrection()
            );

            System.out.printf("✅ SUCCESS! Decompressed %d bytes\n\n", decompressed.length);

            List<R2007SectionMapParser.SectionMapEntry> sections =
                R2007SectionMapParser.parseSectionMap(decompressed);

            System.out.println("SectionMap entries:");
            for (int i = 0; i < sections.size(); i++) {
                System.out.printf("  [%2d] %s\n", i, sections.get(i));
            }

        } catch (Exception e) {
            System.out.printf("❌ Error: %s\n", e.getMessage());
            e.printStackTrace();
        }
    }
}
