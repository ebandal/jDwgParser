package io.dwg.test;

import io.dwg.core.util.Lz77Decompressor;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Test: Try LZ77 on various chunks of SectionMap page data
 * Strategy: Try decompressing sizeComp (905) bytes from different offsets
 */
public class TestSectionMapRawLZ77Fixed {
    public static void main(String[] args) throws Exception {
        byte[] fileData = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));

        long pageOffset = 0xFC80;
        int sizeComp = 905;
        int sizeUncomp = 2188;

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Test: LZ77 decompression of different 905-byte chunks");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        Lz77Decompressor lz77 = new Lz77Decompressor();

        // Try offset 0 first
        System.out.println("Attempting LZ77 with first 905 bytes from 0x" + Long.toHexString(pageOffset) + ":\n");

        byte[] compressed = new byte[sizeComp];
        System.arraycopy(fileData, (int)pageOffset, compressed, 0, sizeComp);

        try {
            byte[] result = lz77.decompress(compressed, sizeUncomp);
            System.out.printf("✅ SUCCESS at offset 0!\n");
            System.out.printf("   Decompressed to %d bytes\n\n", result.length);

            // Show first few values
            System.out.println("First 64 bytes (hex):");
            for (int i = 0; i < 64; i += 16) {
                System.out.printf("+%02X: ", i);
                for (int j = 0; j < 16 && i+j < result.length; j++) {
                    System.out.printf("%02X ", result[i+j] & 0xFF);
                }
                System.out.println();
            }

            // Try to parse as PageMap format (for comparison)
            System.out.println("\nParsing as PageMap entries (size, pageId pairs):");
            int entryCount = 0;
            for (int i = 0; i + 8 <= result.length && entryCount < 10; i += 8) {
                int val1 = readLE32(result, i);
                int val2 = readLE32(result, i + 4);
                if (val1 == 0 && val2 == 0) break;
                System.out.printf("  [%d] 0x%08X, 0x%08X\n", entryCount, val1, val2);
                entryCount++;
            }

        } catch (Exception e) {
            System.out.printf("❌ Offset 0 failed: %s\n\n", e.getMessage());

            // Try every 256-byte aligned offset
            System.out.println("Trying 256-byte aligned offsets:\n");
            for (int offset = 256; offset < 2048; offset += 256) {
                byte[] tryCompressed = new byte[Math.min(sizeComp, fileData.length - (int)pageOffset - offset)];
                System.arraycopy(fileData, (int)(pageOffset + offset), tryCompressed, 0, tryCompressed.length);

                try {
                    byte[] result = lz77.decompress(tryCompressed, sizeUncomp);
                    System.out.printf("✅ SUCCESS at offset +%d!\n", offset);
                    System.out.printf("   Decompressed %d bytes → %d bytes\n\n", tryCompressed.length, result.length);
                    break;
                } catch (Exception e2) {
                    System.out.printf("❌ Offset +%d: %s\n", offset, e2.getMessage().substring(0, Math.min(40, e2.getMessage().length())));
                }
            }
        }
    }

    static int readLE32(byte[] data, int offset) {
        int v1 = data[offset] & 0xFF;
        int v2 = (data[offset + 1] & 0xFF) << 8;
        int v3 = (data[offset + 2] & 0xFF) << 16;
        int v4 = (data[offset + 3] & 0xFF) << 24;
        return v1 | v2 | v3 | v4;
    }
}
