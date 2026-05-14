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
 * Debug: Show section details including page information
 */
public class DebugSectionDetails {
    public static void main(String[] args) throws Exception {
        byte[] fileData = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));
        ByteBuffer buf = ByteBuffer.wrap(fileData);
        BitInput input = new ByteBufferBitInput(buf);

        R2007FileHeader header = R2007FileHeader.read(input);

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

        // Find SectionMap
        long cumulativeOffset = 0;
        long sectionMapFileOffset = -1;
        for (R2007PageMapParser.PageMapEntry entry : pageMap) {
            if (entry.pageId == header.sectionsMapId()) {
                sectionMapFileOffset = 0x480L + header.pageMapOffset() + cumulativeOffset;
                break;
            }
            cumulativeOffset += entry.size;
        }

        // Read SectionMap
        byte[] sectionMapDecompressed = R2007SystemPageReader.readSystemPage(
            input,
            sectionMapFileOffset,
            header.sectionsMapSizeComp(),
            header.sectionsMapSizeUncomp(),
            header.sectionsMapCorrection()
        );

        // Parse SectionMap
        List<R2007SectionMapParser.SectionMapEntry> sections =
            R2007SectionMapParser.parseSectionMap(sectionMapDecompressed);

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Arc.dwg - Detailed Section Information");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        System.out.printf("Found %d sections\n\n", sections.size());

        for (int i = 0; i < sections.size(); i++) {
            R2007SectionMapParser.SectionMapEntry s = sections.get(i);
            System.out.printf("Section %d: %s\n", i, s.sectionName);
            System.out.printf("  dataSize=0x%X, maxSize=0x%X, numPages=%d\n",
                s.dataSize, s.maxSize, s.numPages);
            System.out.printf("  encrypted=%d, encoded=%d, hashcode=0x%X\n",
                s.encrypted, s.encoded, s.hashcode);

            if (s.pages.size() > 0) {
                System.out.printf("  Pages: %d entries\n", s.pages.size());
                for (int p = 0; p < s.pages.size(); p++) {
                    R2007SectionMapParser.SectionPageEntry page = s.pages.get(p);
                    System.out.printf("    [%d] offset=0x%X, size=0x%X, id=0x%X, uncomp=0x%X, comp=0x%X\n",
                        p, page.offset, page.size, page.id, page.uncompSize, page.compSize);
                }
            } else {
                System.out.println("  No page entries");
            }
            System.out.println();
        }

        System.out.printf("\nPageMap summary: %d pages, total size: 0x%X bytes\n",
            pageMap.size(), pageMap.stream().mapToLong(e -> e.size).sum());
    }
}
