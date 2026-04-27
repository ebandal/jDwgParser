package io.dwg.test;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Debug: Examine raw PageMap data at offset 0x480
 */
public class DebugPageMapRawData {
    public static void main(String[] args) throws Exception {
        byte[] fileData = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));

        System.out.println("Raw data at 0x480 (first 64 bytes):");
        for (int i = 0; i < 64; i += 16) {
            System.out.printf("+%03X: ", 0x480 + i);
            for (int j = 0; j < 16 && 0x480 + i + j < fileData.length; j++) {
                System.out.printf("%02X ", fileData[0x480 + i + j] & 0xFF);
            }
            System.out.println();
        }

        System.out.println("\n---Looking for pattern (4 blocks, 255 bytes each = 1020 bytes)---\n");

        // Look for separator patterns between blocks
        System.out.println("Byte pattern analysis:");
        int[] counts = new int[256];
        for (int i = 0; i < 1024 && 0x480 + i < fileData.length; i++) {
            counts[fileData[0x480 + i] & 0xFF]++;
        }

        // Find most common bytes
        System.out.println("Most common bytes in 1024-byte region:");
        for (int b = 0; b < 5; b++) {
            int maxIdx = -1, maxCnt = -1;
            for (int i = 0; i < 256; i++) {
                if (counts[i] > maxCnt) {
                    maxCnt = counts[i];
                    maxIdx = i;
                }
            }
            if (maxIdx >= 0) {
                System.out.printf("  0x%02X: %d times\n", maxIdx, maxCnt);
                counts[maxIdx] = 0;
            }
        }

        // Check if data looks like it could be RS-encoded (RS should have variance)
        System.out.println("\nStatistics:");
        int zeroCount = counts[0];
        int ffCount = counts[0xFF];
        System.out.printf("  Zero bytes: %d\n", zeroCount);
        System.out.printf("  0xFF bytes: %d\n", ffCount);
        System.out.printf("  Unique byte values: %d\n", (int) java.util.Arrays.stream(counts).filter(x -> x > 0).count());
    }
}
