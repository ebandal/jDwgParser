package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.format.r2007.R2007FileHeader;
import io.dwg.format.r2007.R2007SystemPageReader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Detailed hex dump with byte-by-byte interpretation
 */
public class DebugPageMapHexDump {
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

        System.out.println("Full PageMap Hex Dump (320 bytes, first 160 shown):");
        System.out.println("Offset | Bytes (hex)                      | LE32 interpretation");
        System.out.println("-------|----------------------------------|---------");

        for (int i = 0; i < Math.min(decompressed.length, 160); i += 16) {
            System.out.printf("+%04X  |", i);

            // Print hex
            for (int j = 0; j < 16 && i + j < decompressed.length; j++) {
                System.out.printf(" %02X", decompressed[i + j] & 0xFF);
            }
            System.out.println();

            // Print LE32 values for this row
            System.out.print("       | ");
            for (int j = 0; j + 4 <= 16 && i + j + 4 <= decompressed.length; j += 4) {
                long val = readLE32(decompressed, i + j) & 0xFFFFFFFFL;
                System.out.printf("0x%08X    ", val);
            }
            System.out.println();
        }

        System.out.println("\n\nStructure Check: If it's (size32, pageId32) pairs:");
        System.out.println("Pair | Size(LE32)  | PageId(LE32)");
        System.out.println("-----|-------------|----------");
        for (int i = 0; i + 8 <= Math.min(decompressed.length, 160); i += 8) {
            long size = readLE32(decompressed, i) & 0xFFFFFFFFL;
            long pageId = readLE32(decompressed, i + 4) & 0xFFFFFFFFL;
            System.out.printf("%3d  | 0x%08X  | 0x%08X\n", i / 8, size, pageId);
        }
    }

    static long readLE32(byte[] data, int offset) {
        if (offset + 4 > data.length) return 0;
        long v1 = data[offset] & 0xFF;
        long v2 = (data[offset + 1] & 0xFF) << 8;
        long v3 = (data[offset + 2] & 0xFF) << 16;
        long v4 = (data[offset + 3] & 0xFF) << 24;
        return v1 | v2 | v3 | v4;
    }
}
