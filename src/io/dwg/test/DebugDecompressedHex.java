package io.dwg.test;

import io.dwg.core.util.ReedSolomonDecoder;
import io.dwg.core.util.Lz77Decompressor;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Hexdump the decompressed R2007 header to debug field offsets
 */
public class DebugDecompressedHex {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Hex Dump of Decompressed R2007 Header");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] data = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));

        // Extract and decode RS-encoded header
        byte[] rsEncoded = new byte[0x3d8];
        System.arraycopy(data, 0x80, rsEncoded, 0, 0x3d8);

        byte[] decoded = ReedSolomonDecoder.decodeR2007Data(rsEncoded);
        if (decoded == null) {
            System.out.println("❌ RS decoder failed!");
            return;
        }

        // Read metadata
        int comprLen = readLE32(decoded, 24);
        System.out.printf("compr_len at offset 24: %d bytes\n\n", comprLen);

        // Decompress
        byte[] compressed = new byte[comprLen];
        System.arraycopy(decoded, 32, compressed, 0, comprLen);

        Lz77Decompressor lz77 = new Lz77Decompressor();
        byte[] decompressed = lz77.decompress(compressed, 272);

        System.out.printf("Decompressed %d bytes. Full hex dump:\n\n", decompressed.length);

        for (int i = 0; i < decompressed.length; i += 16) {
            System.out.printf("+%03X: ", i);
            for (int j = 0; j < 16 && i + j < decompressed.length; j++) {
                System.out.printf("%02X ", decompressed[i + j] & 0xFF);
            }
            System.out.println();
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("Field Analysis (assuming struct starts at offset 0):");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        System.out.printf("+00 (CRC1):              0x%016X\n", readLE64(decompressed, 0));
        System.out.printf("+08 (Key):              0x%016X\n", readLE64(decompressed, 8));
        System.out.printf("+10 (CRC2):             0x%016X\n", readLE64(decompressed, 16));
        System.out.printf("+18 (compr_len):        %d\n", readLE32(decompressed, 24));
        System.out.printf("+1C (len2):             %d\n", readLE32(decompressed, 28));
        System.out.printf("+20 (??):               0x%016X\n", readLE64(decompressed, 32));
        System.out.printf("+28 (??):               0x%016X\n", readLE64(decompressed, 40));
        System.out.printf("+30 (??):               0x%016X\n", readLE64(decompressed, 48));
        System.out.printf("+38 (pages_map_offset): 0x%016X\n", readLE64(decompressed, 56));
        System.out.printf("+40 (??):               0x%016X\n", readLE64(decompressed, 64));
        System.out.printf("+48 (??):               0x%016X\n", readLE64(decompressed, 72));
        System.out.printf("+50 (pages_map_comp):   0x%016X\n", readLE64(decompressed, 80));
        System.out.printf("+58 (pages_map_uncomp): 0x%016X\n", readLE64(decompressed, 88));
    }

    private static long readLE64(byte[] data, int offset) {
        if (offset + 8 > data.length) return 0;
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

    private static int readLE32(byte[] data, int offset) {
        if (offset + 4 > data.length) return 0;
        int v1 = data[offset] & 0xFF;
        int v2 = (data[offset + 1] & 0xFF) << 8;
        int v3 = (data[offset + 2] & 0xFF) << 16;
        int v4 = (data[offset + 3] & 0xFF) << 24;
        return v1 | v2 | v3 | v4;
    }
}
