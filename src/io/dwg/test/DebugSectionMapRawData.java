package io.dwg.test;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Debug: Examine raw data at SectionMap page offset
 */
public class DebugSectionMapRawData {
    public static void main(String[] args) throws Exception {
        byte[] fileData = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));

        long offset = 0xFC80;

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Raw data at SectionMap offset 0x" + Long.toHexString(offset));
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        System.out.println("First 128 bytes (hex):");
        for (int i = 0; i < 128; i += 16) {
            System.out.printf("+%04X: ", offset + i);
            for (int j = 0; j < 16 && offset + i + j < fileData.length; j++) {
                System.out.printf("%02X ", fileData[(int)(offset + i + j)] & 0xFF);
            }
            System.out.println();
        }

        System.out.println("\nPattern analysis:");
        System.out.println("Looking for markers or common structures...\n");

        // Check for common patterns
        byte[] data = new byte[128];
        for (int i = 0; i < 128; i++) {
            data[i] = fileData[(int)(offset + i)];
        }

        // Look for ASCII text (section names)
        System.out.println("ASCII analysis:");
        boolean inAscii = false;
        StringBuilder asciiStr = new StringBuilder();
        for (byte b : data) {
            if (b >= 32 && b < 127) {
                asciiStr.append((char) b);
                inAscii = true;
            } else {
                if (inAscii && asciiStr.length() > 3) {
                    System.out.printf("  Text: '%s'\n", asciiStr);
                }
                asciiStr = new StringBuilder();
                inAscii = false;
            }
        }

        // Check if data looks compressed (high entropy) or encrypted
        int zeroCount = 0, ffCount = 0;
        for (byte b : data) {
            if (b == 0) zeroCount++;
            if ((b & 0xFF) == 0xFF) ffCount++;
        }
        System.out.printf("\nByte statistics:\n");
        System.out.printf("  Zero bytes: %d/128\n", zeroCount);
        System.out.printf("  0xFF bytes: %d/128\n", ffCount);
        System.out.printf("  Entropy: %s (high=compressed/encrypted, low=structured)\n",
            (zeroCount < 5 && ffCount < 5) ? "HIGH" : "MEDIUM/LOW");

        // Check for page header pattern (used in R2004)
        System.out.println("\nPage header check:");
        System.out.printf("  First 8 bytes as LE64: 0x%016X\n", readLE64(data, 0));
        System.out.printf("  First 4 bytes as LE32: 0x%08X\n", readLE32(data, 0));
    }

    static int readLE32(byte[] data, int offset) {
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
