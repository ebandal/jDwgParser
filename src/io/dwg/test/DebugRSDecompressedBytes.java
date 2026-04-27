package io.dwg.test;

import io.dwg.core.util.ReedSolomonDecoder;
import io.dwg.core.util.Lz77Decompressor;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Hexdump the decompressed header to look for patterns
 */
public class DebugRSDecompressedBytes {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));

        // Extract and decode RS-encoded header
        byte[] rsEncoded = new byte[0x3d8];
        System.arraycopy(data, 0x80, rsEncoded, 0, 0x3d8);

        byte[] decoded = ReedSolomonDecoder.decodeR2007Data(rsEncoded);
        if (decoded == null) {
            System.out.println("RS decoder failed");
            return;
        }

        int comprLen = readLE32(decoded, 24);
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("RS-Decoded Header Analysis (717 bytes)");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        System.out.println("Bytes 0-31 (Metadata):");
        for (int i = 0; i < 32; i += 16) {
            System.out.printf("  0x%02X: ", i);
            for (int j = 0; j < 16; j++) {
                System.out.printf("%02X ", decoded[i + j] & 0xFF);
            }
            System.out.println();
        }

        System.out.println("\nBytes 32-95 (Raw or compressed data):");
        for (int i = 32; i < 96; i += 16) {
            System.out.printf("  0x%02X: ", i);
            for (int j = 0; j < 16 && i + j < decoded.length; j++) {
                System.out.printf("%02X ", decoded[i + j] & 0xFF);
            }
            System.out.println();
        }

        // If compressed, decompress and analyze
        if (comprLen > 0) {
            System.out.println("\n--- DECOMPRESSING (comprLen=" + comprLen + ") ---\n");

            byte[] compressed = new byte[comprLen];
            System.arraycopy(decoded, 32, compressed, 0, comprLen);

            Lz77Decompressor lz77 = new Lz77Decompressor();
            byte[] decompressed = lz77.decompress(compressed, 255);

            System.out.println("Decompressed bytes 0-31:");
            for (int i = 0; i < 32 && i < decompressed.length; i += 16) {
                System.out.printf("  0x%02X: ", i);
                for (int j = 0; j < 16 && i + j < decompressed.length; j++) {
                    System.out.printf("%02X ", decompressed[i + j] & 0xFF);
                }
                System.out.println();
            }

            System.out.println("\nDecompressed bytes 32-63:");
            for (int i = 32; i < 64 && i < decompressed.length; i += 16) {
                System.out.printf("  0x%02X: ", i);
                for (int j = 0; j < 16 && i + j < decompressed.length; j++) {
                    System.out.printf("%02X ", decompressed[i + j] & 0xFF);
                }
                System.out.println();
            }

            System.out.println("\nDecompressed bytes 64-95:");
            for (int i = 64; i < 96 && i < decompressed.length; i += 16) {
                System.out.printf("  0x%02X: ", i);
                for (int j = 0; j < 16 && i + j < decompressed.length; j++) {
                    System.out.printf("%02X ", decompressed[i + j] & 0xFF);
                }
                System.out.println();
            }

            // Look for patterns
            System.out.println("\n--- PATTERN SEARCH ---\n");

            // Look for small LE32 values that could be offsets
            System.out.println("Potential LE32 offsets (0x400-0x100000):");
            for (int i = 0; i < decompressed.length - 4; i += 4) {
                long val = readLE32(decompressed, i);
                if (val > 0x400 && val < 0x100000) {
                    System.out.printf("  Offset 0x%02X: 0x%X (%d)\n", i, val, val);
                }
            }

            // Look for small LE64 values
            System.out.println("\nPotential LE64 offsets (0x400-0x100000):");
            for (int i = 0; i < decompressed.length - 8; i += 8) {
                long val = readLE64(decompressed, i);
                if (val > 0x400 && val < 0x100000) {
                    System.out.printf("  Offset 0x%02X: 0x%X (%d)\n", i, val, val);
                }
            }

            // Look for ASCII strings
            System.out.println("\nASCII strings (printable sequences):");
            StringBuilder sb = new StringBuilder();
            int strStart = -1;
            for (int i = 0; i < decompressed.length; i++) {
                byte b = decompressed[i];
                if (b >= 32 && b <= 126) {
                    if (strStart < 0) strStart = i;
                    sb.append((char) b);
                } else {
                    if (sb.length() >= 4) {
                        System.out.printf("  Offset 0x%02X: \"%s\"\n", strStart, sb.toString());
                    }
                    sb = new StringBuilder();
                    strStart = -1;
                }
            }
            if (sb.length() >= 4) {
                System.out.printf("  Offset 0x%02X: \"%s\"\n", strStart, sb.toString());
            }

            System.out.println("\n════════════════════════════════════════════════════════════════");
            System.out.println("Observations:");
            System.out.println("- Check if decompressed bytes 0-32 contain valid header structure");
            System.out.println("- Look for pages_map_offset around offset 56 in decompressed data");
            System.out.println("- Verify actual file structure against decoded offsets");
        }
    }

    private static long readLE64(byte[] data, int offset) {
        if (offset + 8 > data.length) return 0;
        return (data[offset] & 0xFFL) |
               ((data[offset + 1] & 0xFFL) << 8) |
               ((data[offset + 2] & 0xFFL) << 16) |
               ((data[offset + 3] & 0xFFL) << 24) |
               ((data[offset + 4] & 0xFFL) << 32) |
               ((data[offset + 5] & 0xFFL) << 40) |
               ((data[offset + 6] & 0xFFL) << 48) |
               ((data[offset + 7] & 0xFFL) << 56);
    }

    private static int readLE32(byte[] data, int offset) {
        if (offset + 4 > data.length) return 0;
        return (data[offset] & 0xFF) |
               ((data[offset + 1] & 0xFF) << 8) |
               ((data[offset + 2] & 0xFF) << 16) |
               ((data[offset + 3] & 0xFF) << 24);
    }
}
