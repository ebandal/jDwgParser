package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.util.Lz77Decompressor;
import io.dwg.format.r2007.R2007FileHeader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Test direct PageMap reading from file without RS decoding
 * Hypothesis: Data at 0x480 might be pre-RS-decoded or have different structure
 */
public class TestPageMapDirect {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Test: Direct PageMap Reading from File");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] fileData = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));
        ByteBuffer buffer = ByteBuffer.wrap(fileData);
        BitInput input = new ByteBufferBitInput(buffer);

        // Get header info
        R2007FileHeader header = R2007FileHeader.read(input);
        System.out.printf("Header: pageMapOffset=0x%X, comp=0x%X, uncomp=0x%X\n\n",
            header.pageMapOffset(), header.pageMapSizeComp(), header.pageMapSizeUncomp());

        // Try to read PageMap data directly from offset 0x480 + pages_map_offset
        long pageMapFileOffset = 0x480L + header.pageMapOffset();
        int pageMapSizeComp = (int)(header.pageMapSizeComp() & 0xFFFFFFFFL);
        int pageMapSizeUncomp = (int)(header.pageMapSizeUncomp() & 0xFFFFFFFFL);

        System.out.printf("PageMap location: 0x%X\n", pageMapFileOffset);
        System.out.printf("Reading %d bytes of compressed data...\n\n", pageMapSizeComp);

        byte[] compressedData = new byte[pageMapSizeComp];
        System.arraycopy(fileData, (int)pageMapFileOffset, compressedData, 0, pageMapSizeComp);

        // Try different approaches
        tryApproach1_DirectLZ77(compressedData, pageMapSizeUncomp);
        tryApproach2_SkipFirstByte(compressedData, pageMapSizeUncomp);
        tryApproach3_HexAnalysis(compressedData);
    }

    private static void tryApproach1_DirectLZ77(byte[] compressed, int expectedSize) {
        System.out.println("Approach 1: Direct LZ77 decompression");
        System.out.println("First 16 bytes of compressed data:");
        for (int i = 0; i < 16; i++) {
            System.out.printf("%02X ", compressed[i] & 0xFF);
        }
        System.out.println("\n");

        try {
            Lz77Decompressor lz77 = new Lz77Decompressor();
            byte[] decompressed = lz77.decompress(compressed, expectedSize);
            System.out.printf("✅ Decompressed %d bytes\n\n", decompressed.length);

            // Check if it looks like RLL data
            System.out.println("First few LE64 values:");
            for (int i = 0; i < Math.min(8, decompressed.length - 8); i += 8) {
                long val = readLE64(decompressed, i);
                System.out.printf("+%02d: 0x%016X\n", i, val);
            }
            System.out.println();

        } catch (Exception e) {
            System.out.printf("❌ Error: %s\n\n", e.getMessage());
        }
    }

    private static void tryApproach2_SkipFirstByte(byte[] compressed, int expectedSize) {
        System.out.println("Approach 2: Skip first byte and try LZ77");

        if (compressed.length < 2) {
            System.out.println("❌ Data too short\n");
            return;
        }

        byte[] data = new byte[compressed.length - 1];
        System.arraycopy(compressed, 1, data, 0, data.length);

        try {
            Lz77Decompressor lz77 = new Lz77Decompressor();
            byte[] decompressed = lz77.decompress(data, expectedSize);
            System.out.printf("✅ Decompressed %d bytes\n\n", decompressed.length);
        } catch (Exception e) {
            System.out.printf("❌ Error: %s\n\n", e.getMessage());
        }
    }

    private static void tryApproach3_HexAnalysis(byte[] data) {
        System.out.println("Approach 3: Hex pattern analysis");
        System.out.printf("First 32 bytes hex dump:\n");
        for (int i = 0; i < Math.min(32, data.length); i++) {
            System.out.printf("%02X ", data[i] & 0xFF);
            if ((i + 1) % 16 == 0) System.out.println();
        }
        System.out.println("\n");

        // Look for patterns
        System.out.println("Pattern analysis:");
        int zeroCount = 0;
        int ffCount = 0;
        for (byte b : data) {
            if (b == 0) zeroCount++;
            if ((b & 0xFF) == 0xFF) ffCount++;
        }
        System.out.printf("  Zero bytes: %d\n", zeroCount);
        System.out.printf("  0xFF bytes: %d\n", ffCount);
        System.out.println();
    }

    private static long readLE64(byte[] data, int offset) {
        long value = 0;
        for (int i = 0; i < 8 && offset + i < data.length; i++) {
            value |= ((long)(data[offset + i] & 0xFF)) << (i * 8);
        }
        return value;
    }
}
