package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.format.r2007.R2007FileHeader;
import io.dwg.format.r2007.R2007PageMapParser;
import io.dwg.format.r2007.R2007PageMapParser.PageMapEntry;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Test PageMap extraction and parsing
 */
public class TestR2007PageMapExtraction {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Testing R2007 PageMap Extraction and Parsing");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        String filePath = "./samples/2007/Arc.dwg";
        System.out.printf("Testing: %s\n\n", filePath);

        try {
            byte[] fileData = Files.readAllBytes(Paths.get(filePath));
            ByteBuffer buffer = ByteBuffer.wrap(fileData);
            BitInput input = new ByteBufferBitInput(buffer);

            // Extract header
            R2007FileHeader header = R2007FileHeader.read(input);

            System.out.printf("Header fields:\n");
            System.out.printf("  pages_map_offset:       0x%X\n", header.pageMapOffset());
            System.out.printf("  pages_map_size_comp:    0x%X (%d bytes)\n",
                header.pageMapSizeComp(), header.pageMapSizeComp());
            System.out.printf("  pages_map_size_uncomp:  0x%X (%d bytes)\n\n",
                header.pageMapSizeUncomp(), header.pageMapSizeUncomp());

            // Extract PageMap from file
            // Data section starts at 0x480, plus pages_map_offset gives us the location
            long pageMapFileOffset = 0x480 + header.pageMapOffset();
            System.out.printf("PageMap location in file: 0x%X\n", pageMapFileOffset);

            byte[] pageMapData = R2007PageMapParser.extractPageMapData(fileData,
                pageMapFileOffset,
                (int)(header.pageMapSizeComp() & 0xFFFFFFFFL),
                (int)(header.pageMapSizeUncomp() & 0xFFFFFFFFL),
                header.pageMapSizeComp() > 0);

            System.out.printf("✅ Extracted PageMap: %d bytes decompressed\n\n", pageMapData.length);

            // Parse PageMap
            List<PageMapEntry> pages = R2007PageMapParser.parsePageMap(pageMapData);

            System.out.printf("PageMap entries (%d pages):\n", pages.size());
            for (int i = 0; i < Math.min(10, pages.size()); i++) {
                PageMapEntry page = pages.get(i);
                System.out.printf("  [%d] %s\n", i, page);
            }

            if (pages.size() > 10) {
                System.out.printf("  ... and %d more pages\n", pages.size() - 10);
            }

        } catch (Exception e) {
            System.out.printf("❌ Error: %s\n", e.getMessage());
            e.printStackTrace();
        }
    }
}
