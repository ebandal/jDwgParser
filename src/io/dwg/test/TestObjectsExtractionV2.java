package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.util.Lz77Decompressor;
import io.dwg.format.r2007.R2007FileHeader;
import io.dwg.format.r2007.R2007SystemPageReader;
import io.dwg.format.r2007.R2007PageMapParser;
import io.dwg.format.r2007.R2007SectionMapParser;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Test Objects section extraction (based on working ExtractObjectsCorrect approach)
 */
public class TestObjectsExtractionV2 {
    public static void main(String[] args) throws Exception {
        // Find all R2007 sample files
        Path samplesDir = Paths.get("./samples/2007");
        List<Path> r2007Files;
        try (Stream<Path> stream = Files.list(samplesDir)) {
            r2007Files = stream.filter(p -> p.toString().endsWith(".dwg"))
                .sorted()
                .collect(Collectors.toList());
        }

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Test Objects Extraction V2 (All Files)");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        int successCount = 0, failCount = 0;

        for (Path filePath : r2007Files) {
            try {
                byte[] fileData = Files.readAllBytes(filePath);
                ByteBuffer buf = ByteBuffer.wrap(fileData);
                BitInput input = new ByteBufferBitInput(buf);

                R2007FileHeader header = R2007FileHeader.read(input);

                // Read PageMap
                long pageMapFileOffset = 0x480L + header.pageMapOffset();
                byte[] pageMapDecompressed = R2007SystemPageReader.readSystemPage(
                    input, pageMapFileOffset, header.pageMapSizeComp(),
                    header.pageMapSizeUncomp(), header.pageMapCorrection());

                List<R2007PageMapParser.PageMapEntry> pageMap =
                    R2007PageMapParser.parsePageMap(pageMapDecompressed);

                // Find SectionMap
                long cumulativeOffset = 0, sectionMapFileOffset = -1;
                for (R2007PageMapParser.PageMapEntry entry : pageMap) {
                    if (entry.pageId == header.sectionsMapId()) {
                        sectionMapFileOffset = 0x480L + header.pageMapOffset() + cumulativeOffset;
                        break;
                    }
                    cumulativeOffset += entry.size;
                }

                if (sectionMapFileOffset < 0) {
                    System.out.printf("❌ %s - SectionMap not found\n", filePath.getFileName());
                    failCount++;
                    continue;
                }

                // Read SectionMap
                byte[] sectionMapDecompressed = R2007SystemPageReader.readSystemPage(
                    input, sectionMapFileOffset, header.sectionsMapSizeComp(),
                    header.sectionsMapSizeUncomp(), header.sectionsMapCorrection());

                List<R2007SectionMapParser.SectionMapEntry> sections =
                    R2007SectionMapParser.parseSectionMap(sectionMapDecompressed);

                // Find Objects section (Section 6)
                R2007SectionMapParser.SectionMapEntry objectsSection = null;
                for (int i = 0; i < sections.size(); i++) {
                    if (i == 6) {
                        objectsSection = sections.get(i);
                        break;
                    }
                }

                if (objectsSection == null) {
                    System.out.printf("❌ %s - Objects section not found\n", filePath.getFileName());
                    failCount++;
                    continue;
                }

                // Extract Objects data using EXACT same logic as ExtractObjectsCorrect
                byte[] allObjectsData = new byte[(int)objectsSection.dataSize];
                int objectsOffset = 0;
                boolean success = true;

                for (int p = 0; p < objectsSection.pages.size(); p++) {
                    R2007SectionMapParser.SectionPageEntry page = objectsSection.pages.get(p);

                    // Find in PageMap
                    long filePageOffset = -1, pageSize = -1, cumul = 0;
                    for (R2007PageMapParser.PageMapEntry pm : pageMap) {
                        if (pm.pageId == page.id) {
                            filePageOffset = 0x480L + header.pageMapOffset() + cumul;
                            pageSize = pm.size;
                            break;
                        }
                        cumul += pm.size;
                    }

                    if (filePageOffset < 0) continue;

                    // Read RS-encoded page data
                    byte[] rsData = new byte[(int)pageSize];
                    System.arraycopy(fileData, (int)filePageOffset, rsData, 0, (int)pageSize);

                    // RS decode: R2007 data pages use RS(255, 251)
                    // Per libredwg: round compSize to multiple of 8, then calculate blockCount
                    long pesize = (page.compSize + 7) & ~7L;
                    long blockCount = (pesize + 250) / 251;

                    // Deinterleave and RS decode
                    byte[][] blocks = new byte[(int)blockCount][255];
                    for (int i = 0; i < blockCount; i++) {
                        for (int j = 0; j < 255; j++) {
                            int srcOffset = i + j * (int)blockCount;
                            if (srcOffset < rsData.length) {
                                blocks[i][j] = rsData[srcOffset];
                            }
                        }
                    }

                    // RS decode: Full error correction not yet implemented
                    // For now, use deinterleaved blocks directly

                    // Extract 251 bytes from each block
                    byte[] pedata = new byte[(int)blockCount * 251];
                    for (int i = 0; i < blockCount; i++) {
                        System.arraycopy(blocks[i], 0, pedata, (int)i * 251, 251);
                    }

                    // LZ77 decompress
                    byte[] decompressed;
                    if (page.compSize < page.uncompSize) {
                        try {
                            Lz77Decompressor lz77 = new Lz77Decompressor();
                            decompressed = lz77.decompress(pedata, (int)page.uncompSize);
                        } catch (Exception e) {
                            System.out.printf("❌ %s - LZ77 error page %d: %s\n",
                                filePath.getFileName(), p, e.getMessage());
                            success = false;
                            break;
                        }
                    } else {
                        decompressed = pedata;
                    }

                    // Copy to buffer
                    if (objectsOffset + decompressed.length <= allObjectsData.length) {
                        System.arraycopy(decompressed, 0, allObjectsData, objectsOffset,
                            decompressed.length);
                        objectsOffset += decompressed.length;
                    }
                }

                if (success && objectsOffset == objectsSection.dataSize) {
                    System.out.printf("✅ %s - %d bytes\n", filePath.getFileName(), objectsOffset);
                    successCount++;
                } else if (success) {
                    System.out.printf("⚠️  %s - partial %d/%d\n", filePath.getFileName(),
                        objectsOffset, objectsSection.dataSize);
                    failCount++;
                } else {
                    failCount++;
                }

            } catch (Exception e) {
                System.out.printf("❌ %s - %s\n", filePath.getFileName(), e.getMessage());
                failCount++;
            }
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.printf("Results: %d/%d (%d failed)\n", successCount, successCount + failCount, failCount);
    }
}
