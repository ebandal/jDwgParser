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
import java.nio.file.Paths;
import java.util.List;

/**
 * Extract and decompress Objects section from R2007 file
 */
public class ExtractObjectsSection {
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
        System.out.println("Extract and Decompress Objects Section");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        // Find Objects section (Section 6)
        R2007SectionMapParser.SectionMapEntry objectsSection = null;
        for (int i = 0; i < sections.size(); i++) {
            R2007SectionMapParser.SectionMapEntry s = sections.get(i);
            if (i == 6 || s.dataSize > 100000) { // Section 6 or large section
                objectsSection = s;
                System.out.printf("Found Objects section at index %d:\n", i);
                System.out.printf("  Name: %s\n", s.sectionName);
                System.out.printf("  dataSize: 0x%X (%d bytes)\n", s.dataSize, s.dataSize);
                System.out.printf("  numPages: %d\n", s.numPages);
                System.out.printf("  encrypted: %d, encoded: %d\n", s.encrypted, s.encoded);
                System.out.println();
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
            System.out.printf("  SectionMap: offset=0x%X, size=0x%X\n", page.offset, page.size);
            System.out.printf("  uncomp: 0x%X, comp: 0x%X\n", page.uncompSize, page.compSize);

            // Find page in PageMap to get actual file offset and size
            long filePageOffset = -1;
            long actualPageSize = -1;
            long cumulative = 0;
            for (R2007PageMapParser.PageMapEntry pm : pageMap) {
                if (pm.pageId == page.id) {
                    filePageOffset = 0x480L + header.pageMapOffset() + cumulative;
                    actualPageSize = pm.size;
                    break;
                }
                cumulative += pm.size;
            }

            if (filePageOffset < 0) {
                System.out.printf("  ❌ Page 0x%X not found in PageMap!\n", page.id);
                continue;
            }

            System.out.printf("  PageMap: offset=0x%X, size=0x%X\n", filePageOffset, actualPageSize);

            // Read page data (use actual PageMap size, not SectionMap size)
            byte[] pageData = new byte[(int)actualPageSize];
            System.arraycopy(fileData, (int)filePageOffset, pageData, 0, (int)actualPageSize);

            // Decompress if needed
            byte[] decompressed;
            if (page.compSize > 0 && page.compSize < page.uncompSize) {
                System.out.printf("  Decompressing %d → %d bytes\n", page.compSize, page.uncompSize);
                try {
                    Lz77Decompressor lz77 = new Lz77Decompressor();
                    // Only use compressed size bytes from page data
                    byte[] compressed = new byte[(int)Math.min(page.compSize, pageData.length)];
                    System.arraycopy(pageData, 0, compressed, 0, compressed.length);
                    decompressed = lz77.decompress(compressed, (int)page.uncompSize);
                    System.out.printf("  ✅ Decompressed successfully: %d bytes\n", decompressed.length);
                } catch (Exception e) {
                    System.out.printf("  ❌ Decompression failed: %s\n", e.getMessage());
                    continue;
                }
            } else {
                System.out.printf("  No compression needed (comp=%d, uncomp=%d)\n", page.compSize, page.uncompSize);
                // Use uncompressed data
                decompressed = new byte[(int)Math.min(page.uncompSize, pageData.length)];
                System.arraycopy(pageData, 0, decompressed, 0, decompressed.length);
            }

            // Copy to combined buffer
            if (objectsOffset + decompressed.length <= allObjectsData.length) {
                System.arraycopy(decompressed, 0, allObjectsData, objectsOffset, decompressed.length);
                objectsOffset += decompressed.length;
            } else {
                System.out.printf("  ⚠️ Data doesn't fit in buffer (available: %d, got: %d)\n",
                    allObjectsData.length - objectsOffset, decompressed.length);
            }

            System.out.println();
        }

        System.out.printf("═══════════════════════════════════════════════════════════════\n");
        System.out.printf("Total Objects data extracted: %d bytes (expected: %d)\n", objectsOffset, objectsSection.dataSize);

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
