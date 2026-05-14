package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.util.ByteUtils;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Debug R2007 header parsing.
 */
public class DebugR2007Header {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Debugging R2007 Header Parsing");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] data = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));

        System.out.printf("File size: %d bytes\n\n", data.length);

        // Parse R2007 header
        System.out.println("Header bytes (first 0x80):");
        for (int i = 0; i < 0x80; i += 16) {
            System.out.printf("0x%02X: ", i);
            for (int j = 0; j < 16 && i + j < 0x80; j++) {
                System.out.printf("%02X ", data[i + j] & 0xFF);
            }
            System.out.println();
        }

        System.out.println("\nKey Fields:");

        // Version
        String version = new String(data, 0, 6);
        System.out.printf("  Version (0x00-0x05): %s\n", version);

        // Page map offset - this is supposed to be at 0x20
        long pageMapOffset = ByteUtils.readLE64(data, 0x20);
        System.out.printf("  PageMapOffset (0x20): 0x%X (%d)\n", pageMapOffset, pageMapOffset);

        // Check if offset is reasonable
        if (pageMapOffset > 0 && pageMapOffset < data.length) {
            System.out.printf("  ✓ PageMapOffset is valid\n");
        } else if (pageMapOffset == 0) {
            System.out.printf("  ❌ PageMapOffset is 0 - THIS CAUSES readSections() TO RETURN EMPTY\n");
        } else {
            System.out.printf("  ❌ PageMapOffset is out of range: 0x%X vs file size %d\n", pageMapOffset, data.length);
        }

        // Try alternate location (0x28 is sometimes page map offset)
        long altPageMapOffset = ByteUtils.readLE64(data, 0x28);
        System.out.printf("\n  Alternate PageMapOffset (0x28): 0x%X (%d)\n", altPageMapOffset, altPageMapOffset);

        // Try reading at 0x30
        long altPageMapOffset2 = ByteUtils.readLE64(data, 0x30);
        System.out.printf("  Alternate PageMapOffset (0x30): 0x%X (%d)\n", altPageMapOffset2, altPageMapOffset2);

        System.out.println("\n═══════════════════════════════════════════════════════════════");
    }
}
