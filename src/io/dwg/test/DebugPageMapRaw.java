package io.dwg.test;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Debug raw PageMap data at offset 0x480
 */
public class DebugPageMapRaw {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Raw data at offset 0x480 (potential PageMap start)");
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
        System.out.println("Trying to interpret as (pageId: LE32, size: LE32) pairs:");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        for (int i = 0; i < 32 && offset + i + 8 < data.length; i += 8) {
            int pageId = readLE32(data, offset + i);
            int size = readLE32(data, offset + i + 4);

            System.out.printf("+%03X: pageId=0x%08X, size=0x%08X", i, pageId, size);

            // Check if this looks like valid page structure
            if (pageId > 0 && pageId < 1000 && size > 0 && size < 0x100000) {
                System.out.print(" [VALID?]");
            } else if (pageId == 0 && size == 0) {
                System.out.print(" [END]");
            } else {
                System.out.print(" [??]");
            }

            System.out.println();
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
