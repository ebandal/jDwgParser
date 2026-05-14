package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.format.r2007.R2007FileHeader;
import io.dwg.format.r2007.R2007SystemPageReader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Debug: Hexdump the decompressed PageMap data
 */
public class DebugPageMapDecompressed {
    public static void main(String[] args) throws Exception {
        byte[] fileData = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));
        ByteBuffer buf = ByteBuffer.wrap(fileData);
        BitInput input = new ByteBufferBitInput(buf);

        R2007FileHeader header = R2007FileHeader.read(input);

        long sizeComp = header.pageMapSizeComp();
        long sizeUncomp = header.pageMapSizeUncomp();
        long repeatCount = header.pageMapCorrection();
        long pageMapFileOffset = 0x480L + header.pageMapOffset();

        byte[] decompressed = R2007SystemPageReader.readSystemPage(
            input, pageMapFileOffset, sizeComp, sizeUncomp, repeatCount
        );

        System.out.println("Decompressed PageMap (" + decompressed.length + " bytes):");
        for (int i = 0; i < Math.min(decompressed.length, 160); i += 16) {
            System.out.printf("+%02X: ", i);
            for (int j = 0; j < 16 && i + j < decompressed.length; j++) {
                System.out.printf("%02X ", decompressed[i + j] & 0xFF);
            }
            System.out.println();
        }

        System.out.println("\nPageMap as (LE32, LE32) pairs:");
        for (int i = 0; i + 8 <= Math.min(decompressed.length, 160); i += 8) {
            int pageId = readLE32(decompressed, i);
            int size = readLE32(decompressed, i + 4);
            System.out.printf("  [%2d] pageId=0x%X, size=0x%X\n", i / 8, pageId, size);
            if (pageId == 0 && size == 0) break;
        }
    }

    static int readLE32(byte[] data, int offset) {
        int v1 = data[offset] & 0xFF;
        int v2 = (data[offset + 1] & 0xFF) << 8;
        int v3 = (data[offset + 2] & 0xFF) << 16;
        int v4 = (data[offset + 3] & 0xFF) << 24;
        return v1 | v2 | v3 | v4;
    }
}
