package io.dwg.test;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Debug R2007 header structure at the byte level
 */
public class DebugR2007RawHeader {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("R2007 Raw Header Analysis");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        // Version string
        System.out.printf("Version (0x00-0x05): %s\n\n", new String(data, 0, 6));

        // Unencrypted header (0x06-0x7F)
        System.out.println("Unencrypted Header (0x06-0x7F):");
        System.out.println("  First 64 bytes:");
        for (int i = 0; i < 64; i += 16) {
            System.out.printf("    0x%02X: ", 0x06 + i);
            for (int j = 0; j < 16 && (0x06 + i + j) < 0x80; j++) {
                System.out.printf("%02X ", data[0x06 + i + j] & 0xFF);
            }
            System.out.println();
        }

        // RS-encoded header (0x80-0x3d7)
        System.out.println("\nRS-Encoded Header (0x80-0x3d7, 0x3d8=952 bytes):");
        System.out.println("  First 64 bytes:");
        for (int i = 0; i < 64; i += 16) {
            System.out.printf("    0x%02X: ", 0x80 + i);
            for (int j = 0; j < 16; j++) {
                System.out.printf("%02X ", data[0x80 + i + j] & 0xFF);
            }
            System.out.println();
        }

        // Try to find recognizable patterns
        System.out.println("\nLooking for patterns (0x7C at offset 0x60 in unencrypted header should be data size):");
        long val60 = readLE64(data, 0x60);
        System.out.printf("  Value @ 0x60: 0x%016X (%d)\n", val60, val60);

        long val68 = readLE64(data, 0x68);
        System.out.printf("  Value @ 0x68: 0x%016X (%d)\n", val68, val68);

        long val70 = readLE64(data, 0x70);
        System.out.printf("  Value @ 0x70: 0x%016X (%d)\n", val70, val70);

        System.out.println("\n═══════════════════════════════════════════════════════════════");
    }

    private static long readLE64(byte[] data, int offset) {
        long v1 = data[offset] & 0xFFL;
        long v2 = (data[offset + 1] & 0xFFL) << 8;
        long v3 = (data[offset + 2] & 0xFFL) << 16;
        long v4 = (data[offset + 3] & 0xFFL) << 24;
        long v5 = (data[offset + 4] & 0xFFL) << 32;
        long v6 = (data[offset + 5] & 0xFFL) << 40;
        long v7 = (data[offset + 6] & 0xFFL) << 48;
        long v8 = (data[offset + 7] & 0xFFL) << 56;
        return v1 | v2 | v3 | v4 | v5 | v6 | v7 | v8;
    }
}
