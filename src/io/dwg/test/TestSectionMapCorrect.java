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
 * Test: Read SectionMap using corrected method
 * SectionMap is a DATA PAGE (LZ77-compressed), not a SYSTEM PAGE (RS-encoded)
 */
public class TestSectionMapCorrect {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Test: SectionMap with LZ77 decompression (correct approach)");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] fileData = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));
        ByteBuffer buf = ByteBuffer.wrap(fileData);
        BitInput input = new ByteBufferBitInput(buf);

        R2007FileHeader header = R2007FileHeader.read(input);

        int sectionMapPageId = (int)(header.sectionsMapId() & 0xFFFFFFFF);
        long dataSection = 0x480L + header.pageMapOffset();

        // Read PageMap to find SectionMap page offset
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

        // Find page 0x13
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
        int pageSize = sectionMapPage.size;

        System.out.printf("SectionMap page (0x%X) located:\n", sectionMapPageId);
        System.out.printf("  File offset: 0x%X\n", sectionMapFileOffset);
        System.out.printf("  Page size: 0x%X (%d bytes)\n", pageSize, pageSize);
        System.out.printf("  Compressed size: %d bytes\n", header.sectionsMapSizeComp());
        System.out.printf("  Uncompressed size: %d bytes\n\n", header.sectionsMapSizeUncomp());

        // Read page data
        byte[] pageData = new byte[Math.min(pageSize, fileData.length - (int)sectionMapFileOffset)];
        System.arraycopy(fileData, (int)sectionMapFileOffset, pageData, 0, pageData.length);

        System.out.printf("Read %d bytes from file\n", pageData.length);

        // Decompress using corrected method (LZ77, not RS)
        try {
            byte[] decompressed = R2007SectionMapReader.readSectionMapFromPage(
                pageData,
                header.sectionsMapSizeComp(),
                header.sectionsMapSizeUncomp()
            );

            System.out.printf("✅ Decompressed to %d bytes\n\n", decompressed.length);

            // Parse SectionMap
            List<R2007SectionMapParser.SectionMapEntry> sections =
                R2007SectionMapParser.parseSectionMap(decompressed);

            System.out.printf("Parsed %d section entries:\n", sections.size());
            for (int i = 0; i < Math.min(20, sections.size()); i++) {
                System.out.printf("  [%2d] %s\n", i, sections.get(i));
            }
            if (sections.size() > 20) {
                System.out.printf("  ... and %d more\n", sections.size() - 20);
            }

        } catch (Exception e) {
            System.out.printf("❌ Error: %s\n", e.getMessage());
            e.printStackTrace();
        }
    }
}
