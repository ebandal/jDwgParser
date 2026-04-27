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
 * Test: Parse SectionMap assuming data is NOT compressed
 * Maybe R2007 stores section data raw in allocated page space
 */
public class TestSectionMapRaw {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Test: SectionMap as raw (uncompressed) data");
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

        // Find SectionMap page offset
        long cumulativeOffset = 0;
        for (R2007PageMapParser.PageMapEntry entry : pageMap) {
            if (entry.pageId == sectionMapPageId) {
                break;
            }
            cumulativeOffset += entry.size;
        }

        long sectionMapFileOffset = dataSection + cumulativeOffset;

        // Extract sizeUncomp bytes (assumes data is NOT compressed)
        int dataSize = Math.min((int)header.sectionsMapSizeUncomp(), 2188);
        byte[] rawData = new byte[dataSize];
        System.arraycopy(fileData, (int)sectionMapFileOffset, rawData, 0, dataSize);

        System.out.printf("Extracted %d bytes from offset 0x%X\n", dataSize, sectionMapFileOffset);
        System.out.printf("Assuming data is RAW (not compressed)\n\n");

        // Try to parse directly
        try {
            List<R2007SectionMapParser.SectionMapEntry> sections =
                R2007SectionMapParser.parseSectionMap(rawData);

            System.out.printf("Parsed %d entries:\n", sections.size());
            for (int i = 0; i < Math.min(20, sections.size()); i++) {
                System.out.printf("  [%2d] %s\n", i, sections.get(i));
            }

            if (sections.size() > 0) {
                // Check if values look reasonable
                int minPageId = sections.stream().mapToInt(e -> e.pageId).min().orElse(0);
                int maxPageId = sections.stream().mapToInt(e -> e.pageId).max().orElse(0);
                long minSize = sections.stream().mapToLong(e -> e.size).min().orElse(0);
                long maxSize = sections.stream().mapToLong(e -> e.size).max().orElse(0);

                System.out.printf("\nStatistics:\n");
                System.out.printf("  PageIDs range: 0x%X - 0x%X\n", minPageId, maxPageId);
                System.out.printf("  Sizes range: 0x%X - 0x%X bytes\n", minSize, maxSize);

                if (minPageId >= 0 && maxPageId <= 100 && minSize >= 0 && maxSize <= 100000) {
                    System.out.println("\n✅ Values look reasonable!");
                } else {
                    System.out.println("\n⚠️ Values look suspicious");
                }
            }

        } catch (Exception e) {
            System.out.printf("Error: %s\n", e.getMessage());
        }
    }
}
