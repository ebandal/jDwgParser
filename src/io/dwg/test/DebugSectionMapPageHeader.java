package io.dwg.test;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Debug: Check for page header patterns in SectionMap page
 */
public class DebugSectionMapPageHeader {
    public static void main(String[] args) throws Exception {
        byte[] fileData = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));

        long offset = 0xFC80;
        int len = 4864;

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("SectionMap page structure analysis");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        System.out.println("First 256 bytes (hex):");
        for (int i = 0; i < 256; i += 16) {
            System.out.printf("+%04X: ", i);
            for (int j = 0; j < 16; j++) {
                System.out.printf("%02X ", fileData[(int)(offset + i + j)] & 0xFF);
            }
            System.out.println();
        }

        System.out.println("\n\nTrying different starting offsets for LZ77:\n");

        // Try different starting offsets
        for (int skipBytes = 0; skipBytes <= 64; skipBytes += 8) {
            System.out.printf("Offset +%d: ", skipBytes);

            // Check for LZ77 marker (should start with low entropy or specific pattern)
            int b0 = fileData[(int)(offset + skipBytes)] & 0xFF;
            int b1 = fileData[(int)(offset + skipBytes + 1)] & 0xFF;
            int b2 = fileData[(int)(offset + skipBytes + 2)] & 0xFF;

            System.out.printf("first 3 bytes: %02X %02X %02X", b0, b1, b2);

            // Look for common patterns
            if (b0 == 0x78 && b1 == 0x9C) {
                System.out.print(" [looks like ZLIB header]");
            }
            if (b0 == 0x1F && b1 == 0x8B) {
                System.out.print(" [looks like GZIP header]");
            }

            System.out.println();
        }

        System.out.println("\n\nAttempting LZ77 decompression with different offsets:");

        io.dwg.core.util.Lz77Decompressor lz77 = new io.dwg.core.util.Lz77Decompressor();

        for (int skipBytes = 0; skipBytes <= 32; skipBytes += 8) {
            byte[] compressed = new byte[Math.min(905, len - skipBytes)];
            System.arraycopy(fileData, (int)(offset + skipBytes), compressed, 0, compressed.length);

            try {
                byte[] result = lz77.decompress(compressed, 2188);
                System.out.printf("✅ Offset +%d: SUCCESS! Decompressed to %d bytes\n", skipBytes, result.length);
                break;
            } catch (Exception e) {
                System.out.printf("❌ Offset +%d: %s\n", skipBytes, e.getMessage());
            }
        }
    }
}
