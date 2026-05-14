package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.format.r2007.R2007FileHeader;
import io.dwg.format.r2007.R2007SystemPageReader;
import io.dwg.format.r2007.R2007PageMapParser;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Debug: Dump all PageMap entries and verify sectionsMapId location
 */
public class DebugPageMapFull {
    public static void main(String[] args) throws Exception {
        byte[] fileData = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));
        ByteBuffer buf = ByteBuffer.wrap(fileData);
        BitInput input = new ByteBufferBitInput(buf);

        R2007FileHeader header = R2007FileHeader.read(input);

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Full PageMap Dump and sectionsMapId Verification");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        System.out.printf("Header Info:\n");
        System.out.printf("  pageMapOffset:    0x%X (%d)\n", header.pageMapOffset(), header.pageMapOffset());
        System.out.printf("  pageMapSizeComp:  %d\n", header.pageMapSizeComp());
        System.out.printf("  pageMapSizeUncomp: %d\n", header.pageMapSizeUncomp());
        System.out.printf("  pageMapCorrection: %d\n", header.pageMapCorrection());
        System.out.printf("  sectionsMapId:    0x%X (%d)\n", header.sectionsMapId(), header.sectionsMapId());
        System.out.printf("  sectionsMapSizeComp:   %d\n", header.sectionsMapSizeComp());
        System.out.printf("  sectionsMapSizeUncomp: %d\n\n", header.sectionsMapSizeUncomp());

        // Read PageMap
        long pageMapFileOffset = 0x480L + header.pageMapOffset();
        byte[] pageMapDecompressed = R2007SystemPageReader.readSystemPage(
            input,
            pageMapFileOffset,
            header.pageMapSizeComp(),
            header.pageMapSizeUncomp(),
            header.pageMapCorrection()
        );

        System.out.printf("PageMap decompressed: %d bytes\n\n", pageMapDecompressed.length);

        List<R2007PageMapParser.PageMapEntry> pageMap =
            R2007PageMapParser.parsePageMap(pageMapDecompressed);

        System.out.printf("PageMap entries: %d\n", pageMap.size());
        System.out.println("All page entries:");
        long cumulativeOffset = 0;
        for (int i = 0; i < pageMap.size(); i++) {
            R2007PageMapParser.PageMapEntry entry = pageMap.get(i);
            System.out.printf("  [%2d] pageId=0x%02X size=0x%06X (%6d bytes) offset=0x%06X\n",
                i, entry.pageId, entry.size, entry.size, cumulativeOffset);
            cumulativeOffset += entry.size;
        }

        // Find sectionsMapId
        System.out.printf("\nSearching for sectionsMapId=0x%02X...\n", header.sectionsMapId());
        long sectionMapFileOffset = -1;
        long dataSection = 0x480L + header.pageMapOffset();
        cumulativeOffset = 0;
        boolean found = false;

        for (R2007PageMapParser.PageMapEntry entry : pageMap) {
            if (entry.pageId == header.sectionsMapId()) {
                sectionMapFileOffset = dataSection + cumulativeOffset;
                found = true;
                System.out.printf("✅ Found at cumulative offset 0x%X, file offset 0x%X\n",
                    cumulativeOffset, sectionMapFileOffset);
                System.out.printf("   Page size: 0x%X (%d bytes)\n", entry.size, entry.size);
                break;
            }
            cumulativeOffset += entry.size;
        }

        if (!found) {
            System.out.printf("❌ Page 0x%02X not found in PageMap!\n", header.sectionsMapId());
            System.out.printf("   Max page ID in PageMap: 0x%02X\n",
                pageMap.stream().mapToInt(e -> e.pageId).max().orElse(0));
            System.out.printf("   Total pages: %d\n", pageMap.size());
        }

        // Also check by direct calculation
        System.out.printf("\n\nVerification against 0xFC80 (used in other tests):\n");
        System.out.printf("  Calculated offset from PageMap: 0x%X\n", sectionMapFileOffset);
        System.out.printf("  Offset used in tests:           0xFC80\n");
        if (found && sectionMapFileOffset == 0xFC80) {
            System.out.println("  ✅ MATCH!");
        } else if (found) {
            System.out.printf("  ⚠️  MISMATCH - difference: 0x%X bytes\n",
                sectionMapFileOffset - 0xFC80);
        }
    }
}
