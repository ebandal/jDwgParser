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
 * Test system page reading with RS decoding and LZ77 decompression
 */
public class TestSystemPageReading {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Test: System Page Reading (PageMap) with RS Decoding");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] fileData = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));

        // Create fresh BitInput at position 0
        ByteBuffer buf = ByteBuffer.wrap(fileData);
        buf.position(0);
        BitInput input = new ByteBufferBitInput(buf);

        // Read header
        R2007FileHeader header = R2007FileHeader.read(input);

        System.out.printf("Header Summary:\n");
        System.out.printf("  pageMapOffset:    0x%X\n", header.pageMapOffset());
        System.out.printf("  pageMapSizeComp:  %d bytes\n", header.pageMapSizeComp());
        System.out.printf("  pageMapSizeUncomp:%d bytes\n", header.pageMapSizeUncomp());
        System.out.printf("  pageMapCorrection:%d\n\n", header.pageMapCorrection());

        // Calculate system page parameters
        long sizeComp = header.pageMapSizeComp();
        long sizeUncomp = header.pageMapSizeUncomp();
        long repeatCount = header.pageMapCorrection();

        long pesize = ((sizeComp + 7) & ~7L) * repeatCount;
        long blockCount = (pesize + 238) / 239;
        long pageSize = (blockCount * 255 + 7) & ~7L;

        System.out.printf("Calculated Parameters:\n");
        System.out.printf("  pesize:      %d bytes\n", pesize);
        System.out.printf("  blockCount:  %d\n", blockCount);
        System.out.printf("  pageSize:    %d bytes\n\n", pageSize);

        // Try to read system page
        long pageMapFileOffset = 0x480L + header.pageMapOffset();
        System.out.printf("Attempting to read PageMap from offset 0x%X...\n\n", pageMapFileOffset);

        try {
            byte[] decompressed = R2007SystemPageReader.readSystemPage(
                input,
                pageMapFileOffset,
                sizeComp,
                sizeUncomp,
                repeatCount
            );

            System.out.printf("✅ System page decompressed successfully! (%d bytes)\n\n", decompressed.length);

            // Try to parse PageMap
            System.out.println("PageMap Entries:");
            List<R2007PageMapParser.PageMapEntry> pages = R2007PageMapParser.parsePageMap(decompressed);
            for (int i = 0; i < Math.min(pages.size(), 10); i++) {
                R2007PageMapParser.PageMapEntry entry = pages.get(i);
                System.out.printf("  [%d] %s\n", i, entry);
            }
            if (pages.size() > 10) {
                System.out.printf("  ... and %d more entries\n", pages.size() - 10);
            }
            System.out.printf("\nTotal pages: %d\n", pages.size());

        } catch (Exception e) {
            System.out.printf("❌ Error: %s\n", e.getMessage());
            e.printStackTrace();
        }
    }
}
