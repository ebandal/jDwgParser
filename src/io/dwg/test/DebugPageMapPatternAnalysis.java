package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.format.r2007.R2007FileHeader;
import io.dwg.format.r2007.R2007SystemPageReader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Analyze PageMap decompressed data pattern to understand structure
 */
public class DebugPageMapPatternAnalysis {
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

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("PageMap Pattern Analysis");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        // Try different parsing strategies

        System.out.println("Strategy 1: Every 8 bytes = (LE32 id, LE32 value) or (LE32 offset, LE32 reserved)");
        parseAs8BytePairs(decompressed);

        System.out.println("\n\nStrategy 2: Single LE32 values (skip 4-byte padding)");
        parseAsCompactValues(decompressed);

        System.out.println("\n\nStrategy 3: LE64 values (reading all 8 bytes as one int64)");
        parseAsLE64(decompressed);

        System.out.println("\n\nStrategy 4: Check for offset patterns (cumulative or sequential)");
        analyzeAsOffsets(decompressed);
    }

    static void parseAs8BytePairs(byte[] data) {
        System.out.println("  (offset, value) interpretation:");
        for (int i = 0; i + 8 <= Math.min(data.length, 160); i += 8) {
            int val1 = readLE32(data, i);
            int val2 = readLE32(data, i + 4);
            System.out.printf("    [%2d] 0x%08X, 0x%08X\n", i / 8, val1, val2);
        }
    }

    static void parseAsCompactValues(byte[] data) {
        System.out.println("  Single values (with zero padding):");
        for (int i = 0; i + 8 <= Math.min(data.length, 160); i += 8) {
            int val = readLE32(data, i);
            System.out.printf("    [%2d] value=0x%08X\n", i / 8, val);
        }
    }

    static void parseAsLE64(byte[] data) {
        System.out.println("  LE64 values:");
        for (int i = 0; i + 8 <= Math.min(data.length, 160); i += 8) {
            long val = readLE64(data, i);
            System.out.printf("    [%2d] value=0x%016X\n", i / 8, val);
        }
    }

    static void analyzeAsOffsets(byte[] data) {
        System.out.println("  Offset analysis (looking for cumulative pattern):");
        int lastVal = 0;
        for (int i = 0; i + 8 <= Math.min(data.length, 160); i += 8) {
            int val = readLE32(data, i);
            int delta = val - lastVal;
            System.out.printf("    [%2d] value=0x%08X, delta from prev=0x%X (%d)\n", i / 8, val, delta, delta);
            lastVal = val;
        }
    }

    static int readLE32(byte[] data, int offset) {
        if (offset + 4 > data.length) return 0;
        int v1 = data[offset] & 0xFF;
        int v2 = (data[offset + 1] & 0xFF) << 8;
        int v3 = (data[offset + 2] & 0xFF) << 16;
        int v4 = (data[offset + 3] & 0xFF) << 24;
        return v1 | v2 | v3 | v4;
    }

    static long readLE64(byte[] data, int offset) {
        long v1 = readLE32(data, offset) & 0xFFFFFFFFL;
        long v2 = readLE32(data, offset + 4) & 0xFFFFFFFFL;
        return v1 | (v2 << 32);
    }
}
