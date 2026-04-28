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
 * Parse extracted Objects data to count entities (format validation)
 */
public class CountEntitiesFromExtractedObjects {
    public static void main(String[] args) throws Exception {
        Path samplesDir = Paths.get("./samples/2007");
        List<Path> r2007Files;
        try (Stream<Path> stream = Files.list(samplesDir)) {
            r2007Files = stream.filter(p -> p.toString().endsWith(".dwg"))
                .sorted()
                .limit(3)  // Test first 3 files
                .collect(Collectors.toList());
        }

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Parse Extracted Objects Data - Entity Count");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

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

                if (sectionMapFileOffset < 0) continue;

                // Read SectionMap
                byte[] sectionMapDecompressed = R2007SystemPageReader.readSystemPage(
                    input, sectionMapFileOffset, header.sectionsMapSizeComp(),
                    header.sectionsMapSizeUncomp(), header.sectionsMapCorrection());

                List<R2007SectionMapParser.SectionMapEntry> sections =
                    R2007SectionMapParser.parseSectionMap(sectionMapDecompressed);

                // Find Objects section
                R2007SectionMapParser.SectionMapEntry objectsSection = null;
                for (int i = 0; i < sections.size(); i++) {
                    if (i == 6) {
                        objectsSection = sections.get(i);
                        break;
                    }
                }

                if (objectsSection == null) continue;

                // Extract Objects data
                byte[] allObjectsData = new byte[(int)objectsSection.dataSize];
                int objectsOffset = 0;

                for (int p = 0; p < objectsSection.pages.size(); p++) {
                    R2007SectionMapParser.SectionPageEntry page = objectsSection.pages.get(p);

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

                    byte[] rsData = new byte[(int)pageSize];
                    System.arraycopy(fileData, (int)filePageOffset, rsData, 0, (int)pageSize);

                    // Per libredwg: round compSize to multiple of 8, then calculate blockCount
                    long pesize = (page.compSize + 7) & ~7L;
                    long blockCount = (pesize + 250) / 251;
                    byte[][] blocks = new byte[(int)blockCount][255];
                    for (int i = 0; i < blockCount; i++) {
                        for (int j = 0; j < 255; j++) {
                            int srcOffset = i + j * (int)blockCount;
                            if (srcOffset < rsData.length) {
                                blocks[i][j] = rsData[srcOffset];
                            }
                        }
                    }

                    byte[] pedata = new byte[(int)blockCount * 251];
                    for (int i = 0; i < blockCount; i++) {
                        System.arraycopy(blocks[i], 0, pedata, (int)i * 251, 251);
                    }

                    byte[] decompressed;
                    if (page.compSize < page.uncompSize) {
                        try {
                            Lz77Decompressor lz77 = new Lz77Decompressor();
                            decompressed = lz77.decompress(pedata, (int)page.uncompSize);
                        } catch (Exception e) {
                            System.out.printf("⚠️  %s: LZ77 error page %d\n", filePath.getFileName(), p);
                            continue;
                        }
                    } else {
                        decompressed = pedata;
                    }

                    if (objectsOffset + decompressed.length <= allObjectsData.length) {
                        System.arraycopy(decompressed, 0, allObjectsData, objectsOffset,
                            decompressed.length);
                        objectsOffset += decompressed.length;
                    }
                }

                // Parse Objects data as entity records
                int entityCount = 0;
                int bitPos = 0;
                try {
                    ByteBuffer objBuf = ByteBuffer.wrap(allObjectsData, 0, objectsOffset);
                    BitInput objInput = new ByteBufferBitInput(objBuf);

                    // Parse records sequentially
                    while (bitPos < objectsOffset * 8 - 32) {
                        // Try to read size (BL = 32 bits)
                        long size = readBL(objInput);
                        bitPos += 32;

                        if (size <= 0 || size > 10000) {
                            break;  // End of valid records
                        }

                        // Read type code (BL = 32 bits)
                        long typeCode = readBL(objInput);
                        bitPos += 32;

                        if (typeCode < 0 || typeCode > 500) {
                            break;  // Invalid type code
                        }

                        // Skip rest of record data (approximate)
                        int skipBytes = Math.min((int)size - 8, 1024);
                        bitPos += skipBytes * 8;
                        if (skipBytes > 0) {
                            for (int i = 0; i < skipBytes; i++) {
                                objInput.readRawChar();
                            }
                        }

                        entityCount++;
                    }
                } catch (Exception e) {
                    // End of data or parse error
                }

                System.out.printf("📊 %s: %d entities in %d bytes\n",
                    filePath.getFileName(), entityCount, objectsOffset);

            } catch (Exception e) {
                System.out.printf("❌ %s: %s\n", filePath.getFileName(), e.getMessage());
            }
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
    }

    private static long readBL(BitInput input) {
        long val = 0;
        for (int i = 0; i < 32; i++) {
            val |= (long)(input.readBit() ? 1 : 0) << i;
        }
        if ((val & 0x80000000L) != 0) {
            val |= 0xFFFFFFFF00000000L;
        }
        return val;
    }
}
