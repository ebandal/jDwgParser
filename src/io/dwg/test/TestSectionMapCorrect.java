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
 * Test: Read SectionMap using correct sectionsMapCorrection value
 */
public class TestSectionMapCorrect {
    public static void main(String[] args) throws Exception {
        byte[] fileData = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));
        ByteBuffer buf = ByteBuffer.wrap(fileData);
        BitInput input = new ByteBufferBitInput(buf);

        R2007FileHeader header = R2007FileHeader.read(input);

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Test: SectionMap with correct sectionsMapCorrection");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        // First, read PageMap to find SectionMap page location
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
        long sectionMapFileOffset = -1;
        for (R2007PageMapParser.PageMapEntry entry : pageMap) {
            if (entry.pageId == header.sectionsMapId()) {
                sectionMapFileOffset = 0x480L + header.pageMapOffset() + cumulativeOffset;
                break;
            }
            cumulativeOffset += entry.size;
        }

        if (sectionMapFileOffset < 0) {
            System.out.println("❌ SectionMap page not found!");
            return;
        }

        System.out.printf("SectionMap page found at 0x%X\n\n", sectionMapFileOffset);
        System.out.printf("SectionMap parameters from header:\n");
        System.out.printf("  size_comp:    %d\n", header.sectionsMapSizeComp());
        System.out.printf("  size_uncomp:  %d\n", header.sectionsMapSizeUncomp());
        System.out.printf("  correction:   %d\n\n", header.sectionsMapCorrection());

        // Read SectionMap using system page pipeline
        try {
            byte[] sectionMapDecompressed = R2007SystemPageReader.readSystemPage(
                input,
                sectionMapFileOffset,
                header.sectionsMapSizeComp(),
                header.sectionsMapSizeUncomp(),
                header.sectionsMapCorrection()
            );

            System.out.printf("✅ SectionMap decompressed: %d bytes\n\n", sectionMapDecompressed.length);

            // Parse SectionMap using RLL format
            List<R2007SectionMapParser.SectionMapEntry> sections =
                R2007SectionMapParser.parseSectionMap(sectionMapDecompressed);

            System.out.printf("Parsed %d section entries:\n", sections.size());
            for (int i = 0; i < Math.min(15, sections.size()); i++) {
                R2007SectionMapParser.SectionMapEntry s = sections.get(i);
                System.out.printf("  [%2d] %s (pages=%d, encrypted=%d, encoded=%d)\n",
                    i, s.sectionName, s.numPages, s.encrypted, s.encoded);
            }

            if (sections.size() > 15) {
                System.out.println("  ... and " + (sections.size() - 15) + " more");
            }

            // Validate section entries
            if (sections.size() > 0) {
                long minSize = sections.stream().mapToLong(e -> e.dataSize).min().orElse(0);
                long maxSize = sections.stream().mapToLong(e -> e.dataSize).max().orElse(0);
                long minPages = sections.stream().mapToLong(e -> e.numPages).min().orElse(0);
                long maxPages = sections.stream().mapToLong(e -> e.numPages).max().orElse(0);

                System.out.printf("\nStatistics:\n");
                System.out.printf("  Data sizes: 0x%X - 0x%X bytes\n", minSize, maxSize);
                System.out.printf("  Num pages: %d - %d\n", minPages, maxPages);

                // Check if values look reasonable
                if (minSize >= 0 && maxSize <= 1000000 && minPages >= 0 && maxPages <= 100) {
                    System.out.println("\n✅ Section values look reasonable!");
                } else {
                    System.out.println("\n⚠️ Some section values look suspicious");
                }
            }

        } catch (Exception e) {
            System.out.printf("❌ Error: %s\n", e.getMessage());
            e.printStackTrace();
        }
    }
}
