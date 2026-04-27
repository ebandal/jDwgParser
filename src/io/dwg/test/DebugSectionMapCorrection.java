package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.format.r2007.R2007FileHeader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Debug: Check sectionsMapCorrection value
 */
public class DebugSectionMapCorrection {
    public static void main(String[] args) throws Exception {
        byte[] fileData = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));
        ByteBuffer buf = ByteBuffer.wrap(fileData);
        BitInput input = new ByteBufferBitInput(buf);

        R2007FileHeader header = R2007FileHeader.read(input);

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("SectionMap Correction Value Analysis");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        System.out.printf("PageMap Correction:    %d\n", header.pageMapCorrection());
        System.out.printf("SectionMap Correction: %d\n\n", header.sectionsMapCorrection());

        // Calculate expected RS block count for SectionMap
        long sizeComp = header.sectionsMapSizeComp();
        long sizeUncomp = header.sectionsMapSizeUncomp();
        long correction = header.sectionsMapCorrection();

        System.out.printf("SectionMap parameters:\n");
        System.out.printf("  size_comp:    %d\n", sizeComp);
        System.out.printf("  size_uncomp:  %d\n", sizeUncomp);
        System.out.printf("  correction:   %d\n\n", correction);

        // Calculate as per libredwg read_system_page():
        // pesize = ((size_comp + 7) & ~7) * repeat_count
        long pesize = ((sizeComp + 7) & ~7) * correction;
        long blockCount = (pesize + 238) / 239;
        long pageSize = (blockCount * 255 + 7) & ~7;

        System.out.printf("Calculated RS parameters:\n");
        System.out.printf("  pesize (pre-RS):  %d (0x%X)\n", pesize, pesize);
        System.out.printf("  block_count:      %d\n", blockCount);
        System.out.printf("  page_size (RS):   %d (0x%X) bytes\n", pageSize, pageSize);

        // Check if page fits in allocated space
        long pageAllocatedSize = 4864; // 0x1300 from PageMap entry 0x13
        System.out.printf("\nPage space check:\n");
        System.out.printf("  Required for RS encoding: %d bytes\n", pageSize);
        System.out.printf("  Allocated space (PageMap): %d bytes\n", pageAllocatedSize);

        if (pageSize <= pageAllocatedSize) {
            System.out.printf("  ✅ Data fits!\n");
        } else {
            System.out.printf("  ❌ Data DOESN'T fit! Need %d more bytes\n", pageSize - pageAllocatedSize);
        }
    }
}
