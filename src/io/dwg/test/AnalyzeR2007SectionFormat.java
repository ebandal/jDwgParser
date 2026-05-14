package io.dwg.test;

import io.dwg.core.util.ByteUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Analyze actual R2007 SectionMap format by examining raw data.
 */
public class AnalyzeR2007SectionFormat {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Analyzing R2007 SectionMap Format");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] data = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));

        long offset = ByteUtils.readLE64(data, 0x20);
        System.out.printf("Reading from offset: 0x%X\n\n", offset);

        // Hexdump first 200 bytes
        int pos = (int) offset;
        System.out.println("Hex dump (first 200 bytes):");
        for (int i = 0; i < 200; i += 16) {
            System.out.printf("0x%02X: ", i);
            for (int j = 0; j < 16 && i + j < 200; j++) {
                System.out.printf("%02X ", data[pos + i + j] & 0xFF);
            }
            System.out.print("  ");
            for (int j = 0; j < 16 && i + j < 200; j++) {
                byte b = data[pos + i + j];
                char c = (char)(b & 0xFF);
                System.out.print((c >= 32 && c < 127) ? c : '.');
            }
            System.out.println();
        }

        System.out.println("\n---Analyzing Fields---\n");

        // Try to find where "Objects" section name is
        String objectsUtf16 = "Objects";
        byte[] objectsBytes = objectsUtf16.getBytes(StandardCharsets.UTF_16LE);

        System.out.print("Looking for 'Objects' (UTF-16LE): ");
        for (byte b : objectsBytes) {
            System.out.printf("%02X ", b);
        }
        System.out.println();

        int foundAt = -1;
        for (int i = 0; i < 200; i++) {
            boolean match = true;
            for (int j = 0; j < objectsBytes.length && i + j < 200; j++) {
                if (data[pos + i + j] != objectsBytes[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                foundAt = i;
                break;
            }
        }

        if (foundAt >= 0) {
            System.out.printf("\n✓ Found 'Objects' at offset +%d (0x%02X)\n", foundAt, foundAt);
            System.out.println("This is likely where the section name field starts in the descriptor");
        } else {
            System.out.println("\n✗ 'Objects' not found - section name might be different or at different location");

            // Try to find any 4-character UTF-16LE text
            System.out.println("\nLooking for UTF-16LE patterns (XX 00 XX 00):");
            for (int i = 0; i < 100; i += 2) {
                if (i + 3 < 200 && data[pos + i + 1] == 0 && data[pos + i + 3] == 0) {
                    char c1 = (char)(data[pos + i] & 0xFF);
                    char c2 = (char)(data[pos + i + 2] & 0xFF);
                    if (c1 >= 32 && c1 < 127 && c2 >= 32 && c2 < 127) {
                        System.out.printf("  +0x%02X: %c%c... \n", i, c1, c2);
                    }
                }
            }
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
    }
}
