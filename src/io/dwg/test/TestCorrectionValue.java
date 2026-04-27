package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.format.r2007.R2007FileHeader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestCorrectionValue {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("PageMap Correction Value and blockCount Calculation");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] data = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));
        ByteBuffer buf = ByteBuffer.wrap(data);
        BitInput input = new ByteBufferBitInput(buf);

        R2007FileHeader h = R2007FileHeader.read(input);

        System.out.printf("Header values:\n");
        System.out.printf("  pageMapSizeComp:  0x%X (%d)\n", h.pageMapSizeComp(), h.pageMapSizeComp());
        System.out.printf("  pageMapSizeUncomp: 0x%X (%d)\n", h.pageMapSizeUncomp(), h.pageMapSizeUncomp());
        System.out.printf("  pageMapCorrection: 0x%X (%d)\n\n", h.pageMapCorrection(), h.pageMapCorrection());

        // Calculate blockCount using read_system_page logic
        long sizeComp = h.pageMapSizeComp();
        long repeatCount = h.pageMapCorrection();
        long pesize = ((sizeComp + 7) & ~7L) * repeatCount;
        long blockCount = (pesize + 238) / 239;
        long pageSize = (blockCount * 255 + 7) & ~7L;

        System.out.printf("Calculation (per read_system_page):\n");
        System.out.printf("  pesize = ((sizeComp + 7) & ~7) * repeatCount\n");
        System.out.printf("  pesize = ((%d + 7) & ~7) * %d = %d\n", sizeComp, repeatCount, pesize);
        System.out.printf("\n  blockCount = (pesize + 238) / 239\n");
        System.out.printf("  blockCount = (%d + 238) / 239 = %d\n", pesize, blockCount);
        System.out.printf("\n  pageSize = (blockCount * 255 + 7) & ~7\n");
        System.out.printf("  pageSize = (%d * 255 + 7) & ~7 = 0x%X (%d bytes)\n", blockCount, pageSize, pageSize);

        System.out.printf("\n필요한 RS 디코딩:\n");
        System.out.printf("  - 파일에서 %d 바이트 읽기\n", pageSize);
        System.out.printf("  - RS 디코딩: %d 블록, 각 블록 239 바이트\n", blockCount);
        System.out.printf("  - 출력: %d 바이트\n", blockCount * 239);
    }
}
