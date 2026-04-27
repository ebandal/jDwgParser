package io.dwg.test;

import io.dwg.core.util.ReedSolomonDecoder;
import io.dwg.core.util.Lz77Decompressor;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Hexdump decompressed header and search for patterns
 */
public class DebugDecompressedData {
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
        byte[] compressed = new byte[comprLen];
        System.arraycopy(decoded, 32, compressed, 0, comprLen);

        Lz77Decompressor lz77 = new Lz77Decompressor();
        byte[] decompressed = lz77.decompress(compressed, 256);

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Decompressed Header (256 bytes)");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        // Hexdump all 256 bytes
        for (int i = 0; i < decompressed.length; i += 16) {
            System.out.printf("  0x%02X: ", i);
            for (int j = 0; j < 16 && i + j < decompressed.length; j++) {
                System.out.printf("%02X ", decompressed[i + j] & 0xFF);
            }
            System.out.println();
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("Searching for recognizable patterns:");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        // Search for LE64 values that look like reasonable offsets (0x400-0x100000)
        System.out.println("Potential LE64 offsets in range [0x400, 0x100000]:");
        for (int i = 0; i < decompressed.length - 8; i += 8) {
            long val = readLE64(decompressed, i);
            if (val >= 0x400 && val <= 0x100000) {
                System.out.printf("  Offset 0x%02X: 0x%X (%d)\n", i, val, val);
            }
        }

        // Also search at non-8-byte-aligned offsets
        System.out.println("\nPotential LE64 offsets at any offset:");
        for (int i = 0; i < decompressed.length - 8; i++) {
            long val = readLE64(decompressed, i);
            if (val >= 0x400 && val <= 0x100000) {
                System.out.printf("  Offset 0x%02X: 0x%X (%d)\n", i, val, val);
            }
        }

        // Check what we get at the "official" offset 56
        System.out.println("\nOfficial field locations (from libredwg Dwg_R2007_Header):");
        System.out.println("  offset  0 (header_size):           " + readLE64(decompressed, 0));
        System.out.println("  offset  8 (file_size):            " + readLE64(decompressed, 8));
        System.out.println("  offset 16 (pages_map_crc_comp):   " + readLE64(decompressed, 16));
        System.out.println("  offset 24 (pages_map_correction): " + readLE64(decompressed, 24));
        System.out.println("  offset 32 (pages_map_crc_seed):   " + readLE64(decompressed, 32));
        System.out.println("  offset 40 (pages_map2_offset):    " + readLE64(decompressed, 40));
        System.out.println("  offset 48 (pages_map2_id):        " + readLE64(decompressed, 48));
        System.out.println("  offset 56 (pages_map_offset):     " + readLE64(decompressed, 56));
        System.out.println("  offset 64 (pages_map_id):         " + readLE64(decompressed, 64));

        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("Next steps:");
        System.out.println("1. Compare decompressed pattern with known good Arc.dwg offsets");
        System.out.println("2. Try different LZ77 decompression algorithms");
        System.out.println("3. Check if this specific file has unusual compression");
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
