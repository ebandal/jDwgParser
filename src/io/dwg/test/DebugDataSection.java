package io.dwg.test;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Check what's at offset 0x480 (data section start) in Arc.dwg
 */
public class DebugDataSection {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Data at offset 0x480 (data section start)");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        int offset = 0x480;
        System.out.printf("First 256 bytes from offset 0x%X:\n\n", offset);

        for (int i = 0; i < 256; i += 16) {
            System.out.printf("+%03X: ", i);
            for (int j = 0; j < 16 && offset + i + j < data.length; j++) {
                System.out.printf("%02X ", data[offset + i + j] & 0xFF);
            }
            System.out.println();
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("Interpreting as LE32 pairs (page map structure):");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        for (int i = 0; i < 32 && offset + i + 8 < data.length; i += 8) {
            long v1 = readLE32(data, offset + i);
            long v2 = readLE32(data, offset + i + 4);
            System.out.printf("  [%d] pageId=0x%08X, size=0x%08X\n", i/8, v1, v2);
        }
    }

    private static int readLE32(byte[] data, int offset) {
        if (offset + 4 > data.length) return 0;
        int v1 = data[offset] & 0xFF;
        int v2 = (data[offset + 1] & 0xFF) << 8;
        int v3 = (data[offset + 2] & 0xFF) << 16;
        int v4 = (data[offset + 3] & 0xFF) << 24;
        return v1 | v2 | v3 | v4;
    }
}
