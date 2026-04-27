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
 * Test: SectionMap on all R2007 files
 */
public class TestSectionMapMultipleFiles {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Test: SectionMap on all R2007 files");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        java.nio.file.Path samplesDir = Paths.get("./samples/2007");
        int successCount = 0;
        int totalCount = 0;

        for (java.nio.file.Path file : java.nio.file.Files.list(samplesDir)
                .filter(p -> p.toString().endsWith(".dwg"))
                .sorted()
                .toArray(java.nio.file.Path[]::new)) {

            totalCount++;
            try {
                byte[] fileData = Files.readAllBytes(file);
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

                if (sectionMapFileOffset < 0) {
                    System.out.printf("❌ %s: SectionMap page not found\n", file.getFileName());
                    continue;
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

                System.out.printf("✅ %s: %d sections\n", file.getFileName(), sections.size());
                successCount++;

            } catch (Exception e) {
                System.out.printf("❌ %s: %s\n", file.getFileName(), e.getMessage());
            }
        }

        System.out.printf("\n═══════════════════════════════════════════════════════════════\n");
        System.out.printf("Results: %d/%d R2007 files processed successfully\n", successCount, totalCount);
    }
}
