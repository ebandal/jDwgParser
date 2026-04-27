package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.format.r2007.R2007FileHeader;
import io.dwg.format.r2007.R2007SystemPageReader;
import io.dwg.format.r2007.R2007PageMapParser;
import io.dwg.format.r2007.R2007SectionMapParser;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Test: Parse SectionMap directly from data page (without RS decoding)
 * Hypothesis: Data pages contain raw (possibly compressed) data, not RS-encoded
 */
public class TestSectionMapDirectData {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Test: Parse SectionMap directly from data page");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] fileData = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));
        ByteBuffer buf = ByteBuffer.wrap(fileData);
        BitInput input = new ByteBufferBitInput(buf);

        R2007FileHeader header = R2007FileHeader.read(input);

        int sectionMapPageId = (int)(header.sectionsMapId() & 0xFFFFFFFF);
        long dataSection = 0x480L + header.pageMapOffset();

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

        // Calculate offset to page 0x13
        long cumulativeOffset = 0;
        R2007PageMapParser.PageMapEntry sectionMapPage = null;

        for (R2007PageMapParser.PageMapEntry entry : pageMap) {
            if (entry.pageId == sectionMapPageId) {
                sectionMapPage = entry;
                break;
            }
            cumulativeOffset += entry.size;
        }

        if (sectionMapPage == null) {
            System.out.println("❌ SectionMap page not found!");
            return;
        }

        long sectionMapFileOffset = dataSection + cumulativeOffset;
        int expectedSize = sectionMapPage.size;

        System.out.printf("SectionMap page found:\n");
        System.out.printf("  File offset: 0x%X\n", sectionMapFileOffset);
        System.out.printf("  Expected size: 0x%X (%d bytes)\n", expectedSize, expectedSize);
        System.out.printf("  Header field sizes:\n");
        System.out.printf("    sizeComp: %d\n", header.sectionsMapSizeComp());
        System.out.printf("    sizeUncomp: %d\n\n", header.sectionsMapSizeUncomp());

        // Extract page data directly
        byte[] pageData = new byte[Math.min(expectedSize, (int)header.sectionsMapSizeUncomp())];
        if (sectionMapFileOffset + pageData.length > fileData.length) {
            System.out.printf("⚠️ Warning: Page extends beyond file! Truncating.\n");
            System.arraycopy(fileData, (int)sectionMapFileOffset, pageData, 0,
                (int)Math.min(pageData.length, fileData.length - (int)sectionMapFileOffset));
        } else {
            System.arraycopy(fileData, (int)sectionMapFileOffset, pageData, 0, pageData.length);
        }

        System.out.printf("Extracted %d bytes of SectionMap data\n\n", pageData.length);

        // Try to parse as-is (no decompression)
        System.out.println("Attempting to parse SectionMap entries directly:");
        try {
            List<R2007SectionMapParser.SectionMapEntry> sections =
                R2007SectionMapParser.parseSectionMap(pageData);

            System.out.printf("✅ Parsed %d entries:\n", sections.size());
            for (int i = 0; i < sections.size(); i++) {
                System.out.printf("  [%2d] %s\n", i, sections.get(i));
            }
        } catch (Exception e) {
            System.out.printf("❌ Parse error: %s\n", e.getMessage());
        }

        // Also show hex dump of first 64 bytes for analysis
        System.out.println("\nFirst 64 bytes (hex):");
        for (int i = 0; i < 64; i += 16) {
            System.out.printf("+%02X: ", i);
            for (int j = 0; j < 16 && i + j < pageData.length; j++) {
                System.out.printf("%02X ", pageData[i + j] & 0xFF);
            }
            System.out.println();
        }
    }
}
