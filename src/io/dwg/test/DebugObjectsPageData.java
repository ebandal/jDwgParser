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
 * Debug: Analyze raw page data from Objects section
 */
public class DebugObjectsPageData {
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

        // Find Objects section
        R2007SectionMapParser.SectionMapEntry objectsSection = null;
        for (int i = 0; i < sections.size(); i++) {
            R2007SectionMapParser.SectionMapEntry s = sections.get(i);
            if (i == 6) {
                objectsSection = s;
                break;
            }
        }

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Analyze Objects Section Page Data");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        // Analyze each page
        for (int p = 0; p < Math.min(3, objectsSection.pages.size()); p++) {
            R2007SectionMapParser.SectionPageEntry page = objectsSection.pages.get(p);
            System.out.printf("PAGE %d (id=0x%X):\n", p, page.id);
            System.out.printf("  SectionMap: offset=0x%X, size=0x%X, uncomp=0x%X, comp=0x%X\n",
                page.offset, page.size, page.uncompSize, page.compSize);

            // Find in PageMap
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

            System.out.printf("  PageMap:    offset=0x%X, size=0x%X\n", filePageOffset, actualPageSize);

            // Read full page data
            byte[] fullPageData = new byte[(int)actualPageSize];
            System.arraycopy(fileData, (int)filePageOffset, fullPageData, 0, (int)actualPageSize);

            // Read only compressed size
            byte[] compressedData = new byte[(int)Math.min(page.compSize, fullPageData.length)];
            System.arraycopy(fullPageData, 0, compressedData, 0, compressedData.length);

            System.out.printf("\n  First 256 bytes of page data (hex):\n");
            for (int i = 0; i < Math.min(256, fullPageData.length); i += 16) {
                System.out.printf("    0x%04X: ", i);
                for (int j = 0; j < 16 && i + j < fullPageData.length; j++) {
                    System.out.printf("%02X ", fullPageData[i + j] & 0xFF);
                }
                System.out.println();
            }

            // Look for markers
            System.out.printf("\n  Marker analysis:\n");
            // LZ77 start marker
            if (compressedData.length > 0) {
                System.out.printf("    First byte: 0x%02X\n", compressedData[0] & 0xFF);
                if (compressedData.length > 1) {
                    System.out.printf("    First LE32: 0x%08X\n", readLE32(compressedData, 0));
                }
            }

            // Check for CRC32 header (R2004 style)
            if (fullPageData.length >= 32) {
                System.out.printf("    First 32 bytes (possible header):\n      ");
                for (int i = 0; i < 32; i++) {
                    System.out.printf("%02X ", fullPageData[i] & 0xFF);
                }
                System.out.println();
            }

            // Try counting non-zero/printable bytes
            int nonZero = 0, printable = 0;
            for (byte b : fullPageData) {
                if (b != 0) nonZero++;
                if (b >= 32 && b < 127) printable++;
            }
            System.out.printf("    Stats: %d/%d non-zero, %d printable ASCII\n",
                nonZero, fullPageData.length, printable);

            System.out.println();
        }
    }

    private static long readLE32(byte[] data, int offset) {
        if (offset + 4 > data.length) return 0;
        long v1 = data[offset] & 0xFF;
        long v2 = (data[offset + 1] & 0xFF) << 8;
        long v3 = (data[offset + 2] & 0xFF) << 16;
        long v4 = (data[offset + 3] & 0xFF) << 24;
        return v1 | v2 | v3 | v4;
    }
}
