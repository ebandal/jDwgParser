package io.dwg.test;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Detailed hex dump and analysis of SectionMap page at 0xFC80
 */
public class DebugSectionMapHex {
    public static void main(String[] args) throws Exception {
        byte[] fileData = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));

        long offset = 0xFC80;
        int len = 1024;

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("SectionMap page detailed hex analysis at 0xFC80");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        // Show first 1024 bytes in detail
        System.out.println("First 1024 bytes (hex):\n");
        for (int i = 0; i < Math.min(1024, len); i += 16) {
            System.out.printf("0x%05X: ", i);
            for (int j = 0; j < 16; j++) {
                if (i + j < fileData.length) {
                    System.out.printf("%02X ", fileData[(int)(offset + i + j)] & 0xFF);
                } else {
                    System.out.print("   ");
                }
            }
            System.out.print(" | ");
            for (int j = 0; j < 16; j++) {
                if (i + j < fileData.length) {
                    byte b = fileData[(int)(offset + i + j)];
                    if (b >= 32 && b < 127) {
                        System.out.print((char)b);
                    } else {
                        System.out.print(".");
                    }
                } else {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }

        // Try to detect patterns
        System.out.println("\n\nPattern Analysis:\n");

        // Look for repeated bytes
        System.out.println("Most common bytes in first 512 bytes:");
        int[] freq = new int[256];
        for (int i = 0; i < 512 && (offset + i) < fileData.length; i++) {
            freq[fileData[(int)(offset + i)] & 0xFF]++;
        }
        for (int b = 0; b < 256; b++) {
            if (freq[b] > 10) {
                System.out.printf("  0x%02X: %3d occurrences\n", b, freq[b]);
            }
        }

        // Check for markers
        System.out.println("\n\nMarker checks at offset 0xFC80:");
        int b0 = fileData[(int)offset] & 0xFF;
        int b1 = fileData[(int)(offset+1)] & 0xFF;
        int b2 = fileData[(int)(offset+2)] & 0xFF;
        int b3 = fileData[(int)(offset+3)] & 0xFF;

        System.out.printf("First 4 bytes: %02X %02X %02X %02X\n", b0, b1, b2, b3);

        // Check if it looks like compressed data
        if (b0 == 0x78 && b1 == 0x9C) System.out.println("  ⚠️ Looks like ZLIB header");
        if (b0 == 0x1F && b1 == 0x8B) System.out.println("  ⚠️ Looks like GZIP header");
        if (b0 == 0x50 && b1 == 0x4B) System.out.println("  ⚠️ Looks like ZIP header");

        // Read as LE32 and BE32
        int le32_0 = (fileData[(int)offset] & 0xFF) |
                     ((fileData[(int)(offset+1)] & 0xFF) << 8) |
                     ((fileData[(int)(offset+2)] & 0xFF) << 16) |
                     ((fileData[(int)(offset+3)] & 0xFF) << 24);
        int be32_0 = ((fileData[(int)offset] & 0xFF) << 24) |
                     ((fileData[(int)(offset+1)] & 0xFF) << 16) |
                     ((fileData[(int)(offset+2)] & 0xFF) << 8) |
                     (fileData[(int)(offset+3)] & 0xFF);

        System.out.printf("\nFirst 4 bytes as LE32: 0x%08X (%d)\n", le32_0, le32_0);
        System.out.printf("First 4 bytes as BE32: 0x%08X (%d)\n", be32_0, be32_0);

        // Check for text patterns
        System.out.println("\n\nText fragments in first 512 bytes:");
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 512; i++) {
            byte b = fileData[(int)(offset + i)];
            if (b >= 32 && b < 127) {
                text.append((char)b);
            } else {
                if (text.length() > 3) {
                    System.out.println("  \"" + text + "\" at offset +0x" + Integer.toHexString(i - text.length()));
                }
                text.setLength(0);
            }
        }
    }
}
