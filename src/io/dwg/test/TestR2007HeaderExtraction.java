package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.format.r2007.R2007FileHeader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Test R2007 header field extraction (decompression + parsing)
 */
public class TestR2007HeaderExtraction {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Testing R2007 Header Field Extraction");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        String[] testFiles = {
            "./samples/2007/Arc.dwg",
            "./samples/2007/Donut.dwg",
            "./samples/2007/Line.dwg",
            "./samples/2007/ATMOS-DC22S.dwg"
        };

        int successCount = 0;
        int failureCount = 0;

        for (String filePath : testFiles) {
            System.out.printf("Testing: %s\n", filePath);

            try {
                byte[] fileData = Files.readAllBytes(Paths.get(filePath));
                ByteBuffer buffer = ByteBuffer.wrap(fileData);
                BitInput input = new ByteBufferBitInput(buffer);

                R2007FileHeader header = R2007FileHeader.read(input);

                System.out.printf("  ✅ pages_map_offset:       0x%X\n", header.pageMapOffset());
                System.out.printf("     pages_map_size_comp:    0x%X (%d bytes)\n",
                    header.pageMapSizeComp(), header.pageMapSizeComp());
                System.out.printf("     pages_map_size_uncomp:  0x%X (%d bytes)\n",
                    header.pageMapSizeUncomp(), header.pageMapSizeUncomp());
                System.out.printf("     sections_map_id:        0x%X\n", header.sectionsMapId());
                System.out.printf("     sections_map_size_comp: 0x%X\n", header.sectionsMapSizeComp());
                System.out.printf("     sections_map_size_uncomp: 0x%X\n", header.sectionsMapSizeUncomp());
                successCount++;

            } catch (Exception e) {
                System.out.printf("  ❌ Error: %s\n", e.getMessage());
                failureCount++;
            }
            System.out.println();
        }

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.printf("Results: %d success, %d failure\n", successCount, failureCount);
    }
}
