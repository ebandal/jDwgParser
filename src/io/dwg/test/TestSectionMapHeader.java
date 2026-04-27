package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.format.r2007.R2007FileHeader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Check SectionMap header values
 */
public class TestSectionMapHeader {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("R2007 SectionMap Header Values");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] fileData = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));
        ByteBuffer buf = ByteBuffer.wrap(fileData);
        BitInput input = new ByteBufferBitInput(buf);

        R2007FileHeader header = R2007FileHeader.read(input);

        System.out.printf("Header values:\n");
        System.out.printf("  sectionsMapId:        0x%X (%d)\n", header.sectionsMapId(), header.sectionsMapId());
        System.out.printf("  sectionsMapSizeComp:  0x%X (%d)\n", header.sectionsMapSizeComp(), header.sectionsMapSizeComp());
        System.out.printf("  sectionsMapSizeUncomp:0x%X (%d)\n\n", header.sectionsMapSizeUncomp(), header.sectionsMapSizeUncomp());

        // SectionMap uses pageMapCorrection as repeatCount (same structure as PageMap)
        long sizeComp = header.sectionsMapSizeComp();
        long sizeUncomp = header.sectionsMapSizeUncomp();
        long repeatCount = header.pageMapCorrection();  // Same as PageMap

        long pesize = ((sizeComp + 7) & ~7L) * repeatCount;
        long blockCount = (pesize + 238) / 239;
        long pageSize = (blockCount * 255 + 7) & ~7L;

        System.out.printf("Calculated Parameters:\n");
        System.out.printf("  pesize = ((%d + 7) & ~7) * %d = %d\n", sizeComp, repeatCount, pesize);
        System.out.printf("  blockCount = (%d + 238) / 239 = %d\n", pesize, blockCount);
        System.out.printf("  pageSize = (%d * 255 + 7) & ~7 = 0x%X (%d bytes)\n", blockCount, pageSize, pageSize);

        System.out.printf("\nSectionMap file location: 0x480 + 0x%X = 0x%X\n",
            header.sectionsMapId(), 0x480L + header.sectionsMapId());
    }
}
