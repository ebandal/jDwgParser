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
 * Test: Use PageMap to locate SectionMap data
 * Hypothesis: sectionsMapId is a page ID, not file offset
 */
public class TestSectionMapViaPageMap {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Test: Locate SectionMap using PageMap");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] fileData = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));
        ByteBuffer buf = ByteBuffer.wrap(fileData);
        BitInput input = new ByteBufferBitInput(buf);

        R2007FileHeader header = R2007FileHeader.read(input);

        System.out.printf("Header values:\n");
        System.out.printf("  sectionsMapId: 0x%X (%d decimal)\n", header.sectionsMapId(), header.sectionsMapId());
        System.out.printf("  sectionsMapSizeComp: %d bytes\n", header.sectionsMapSizeComp());
        System.out.printf("  sectionsMapSizeUncomp: %d bytes\n\n", header.sectionsMapSizeUncomp());

        // Read PageMap first
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

        System.out.printf("PageMap loaded: %d pages\n\n", pageMap.size());

        // Check if sectionsMapId is a page ID in PageMap
        int sectionMapPageId = (int)(header.sectionsMapId() & 0xFFFFFFFF);
        System.out.printf("Looking for page ID 0x%X in PageMap...\n", sectionMapPageId);

        R2007PageMapParser.PageMapEntry sectionMapPage = null;
        for (R2007PageMapParser.PageMapEntry entry : pageMap) {
            if (entry.pageId == sectionMapPageId) {
                sectionMapPage = entry;
                break;
            }
        }

        if (sectionMapPage != null) {
            System.out.printf("✅ FOUND! Page 0x%X: size=0x%X (%d bytes)\n\n",
                sectionMapPage.pageId, sectionMapPage.size, sectionMapPage.size);

            // Now we know the page exists. But we need to calculate file offset.
            // This is complex because we need to know cumulative offsets of all pages.
            // For now, just note this discovery.

            System.out.println("Next step: Calculate cumulative offset of this page from data section");
            System.out.println("(requires summing sizes of all preceding pages)\n");

        } else {
            System.out.printf("❌ Page 0x%X not found in PageMap\n\n", sectionMapPageId);
            System.out.println("Alternative hypotheses to test:");
            System.out.println("1. sectionsMapId uses different encoding (e.g., system page ID vs data page ID)");
            System.out.println("2. SectionMap is stored at fixed location after PageMap");
            System.out.println("3. SectionMap format is different (not RS-encoded or not interleaved)\n");
        }

        // Print all PageMap entries for reference
        System.out.println("Full PageMap:");
        for (R2007PageMapParser.PageMapEntry entry : pageMap) {
            System.out.printf("  Page 0x%02X: size=0x%04X (%d bytes)\n",
                entry.pageId, entry.size, entry.size);
        }
    }
}
