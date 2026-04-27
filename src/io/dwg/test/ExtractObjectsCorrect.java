package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.util.ReedSolomonDecoder;
import io.dwg.core.util.Lz77Decompressor;
import io.dwg.format.r2007.R2007FileHeader;
import io.dwg.format.r2007.R2007SystemPageReader;
import io.dwg.format.r2007.R2007PageMapParser;
import io.dwg.format.r2007.R2007SectionMapParser;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Extract Objects section with correct RS decoding for data pages
 * R2007 data pages: RS(255,251) decode → LZ77 decompress
 */
public class ExtractObjectsCorrect {
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
        System.out.println("Extract Objects Section (with RS decoding)");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        // Find Objects section
        R2007SectionMapParser.SectionMapEntry objectsSection = null;
        for (int i = 0; i < sections.size(); i++) {
            R2007SectionMapParser.SectionMapEntry s = sections.get(i);
            if (i == 6 || s.dataSize > 100000) {
                objectsSection = s;
                System.out.printf("Found Objects section at index %d\n", i);
                System.out.printf("  dataSize: 0x%X (%d bytes), numPages: %d\n\n",
                    s.dataSize, s.dataSize, s.numPages);
                break;
            }
        }

        if (objectsSection == null) {
            System.out.println("❌ Objects section not found!");
            return;
        }

        // Extract and decompress each page
        byte[] allObjectsData = new byte[(int)objectsSection.dataSize];
        int objectsOffset = 0;

        for (int p = 0; p < objectsSection.pages.size(); p++) {
            R2007SectionMapParser.SectionPageEntry page = objectsSection.pages.get(p);
            System.out.printf("Processing page %d (id=0x%X):\n", p, page.id);

            // Find in PageMap
            long filePageOffset = -1;
            long pageSize = -1;
            long cumulative = 0;
            for (R2007PageMapParser.PageMapEntry pm : pageMap) {
                if (pm.pageId == page.id) {
                    filePageOffset = 0x480L + header.pageMapOffset() + cumulative;
                    pageSize = pm.size;
                    break;
                }
                cumulative += pm.size;
            }

            if (filePageOffset < 0) {
                System.out.printf("  ❌ Page 0x%X not found in PageMap!\n", page.id);
                continue;
            }

            System.out.printf("  File offset: 0x%X, allocated size: 0x%X\n", filePageOffset, pageSize);
            System.out.printf("  SectionMap: comp=0x%X, uncomp=0x%X\n", page.compSize, page.uncompSize);

            // Read RS-encoded page data
            byte[] rsData = new byte[(int)pageSize];
            System.arraycopy(fileData, (int)filePageOffset, rsData, 0, (int)pageSize);

            // RS decode: R2007 data pages use RS(255, 251)
            // block_count = (comp_size + 0xFB - 1) / 0xFB  where 0xFB = 251
            long blockCount = (page.compSize + 0xFB - 1) / 0xFB;
            System.out.printf("  RS: blockCount=%d\n", blockCount);

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

            // RS decode each block (RS(255, 251) → 251 bytes per block)
            for (int i = 0; i < blockCount; i++) {
                int errors = ReedSolomonDecoder.decodeBlock(blocks[i], true);
                if (errors >= 0) {
                    System.out.printf("    Block %d: %d errors\n", i, errors);
                }
            }

            // Extract 251 bytes from each block
            byte[] pedata = new byte[(int)blockCount * 251];
            for (int i = 0; i < blockCount; i++) {
                System.arraycopy(blocks[i], 0, pedata, (int)i * 251, 251);
            }

            System.out.printf("  RS decoded: %d bytes\n", pedata.length);

            // LZ77 decompress
            byte[] decompressed;
            if (page.compSize < page.uncompSize) {
                System.out.printf("  LZ77: decompress %d → %d bytes\n", page.compSize, page.uncompSize);
                try {
                    Lz77Decompressor lz77 = new Lz77Decompressor();
                    decompressed = lz77.decompress(pedata, (int)page.uncompSize);
                    System.out.printf("  ✅ Decompressed: %d bytes\n", decompressed.length);
                } catch (Exception e) {
                    System.out.printf("  ❌ Decompression failed: %s\n", e.getMessage());
                    continue;
                }
            } else {
                System.out.println("  No LZ77 needed");
                decompressed = pedata;
            }

            // Copy to combined buffer
            if (objectsOffset + decompressed.length <= allObjectsData.length) {
                System.arraycopy(decompressed, 0, allObjectsData, objectsOffset, decompressed.length);
                objectsOffset += decompressed.length;
            }

            System.out.println();
        }

        System.out.printf("═══════════════════════════════════════════════════════════════\n");
        System.out.printf("✅ Total Objects data extracted: %d bytes (expected: %d)\n",
            objectsOffset, objectsSection.dataSize);

        // Show first 256 bytes as hex
        System.out.println("\nFirst 256 bytes of Objects data (hex):\n");
        for (int i = 0; i < Math.min(256, allObjectsData.length); i += 16) {
            System.out.printf("0x%04X: ", i);
            for (int j = 0; j < 16 && i + j < allObjectsData.length; j++) {
                System.out.printf("%02X ", allObjectsData[i + j] & 0xFF);
            }
            System.out.println();
        }
    }
}
