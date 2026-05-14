package io.dwg.test;

import io.dwg.core.util.ReedSolomonDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Test the RS decoder with actual R2007 Arc.dwg data
 */
public class TestRSDecoder {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Testing RS Decoder with Arc.dwg");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        // Extract RS-encoded header (0x80-0x457, 0x3d8 bytes)
        byte[] rsEncoded = new byte[0x3d8];
        System.arraycopy(data, 0x80, rsEncoded, 0, 0x3d8);

        System.out.println("RS-Encoded data (0x80-0x457):");
        System.out.printf("  First 64 bytes: ");
        for (int i = 0; i < 64; i++) {
            if (i > 0 && i % 16 == 0) System.out.printf("\n                  ");
            System.out.printf("%02X ", rsEncoded[i] & 0xFF);
        }
        System.out.println("\n");

        // Try decoding
        System.out.println("Decoding using ReedSolomonDecoder.decodeR2007Data()...");
        byte[] decoded = ReedSolomonDecoder.decodeR2007Data(rsEncoded);

        if (decoded == null) {
            System.out.println("  ❌ Decoder returned null!");
        } else {
            System.out.printf("  ✓ Decoded %d bytes\n", decoded.length);
            System.out.println("\n  First 64 bytes of decoded data:");
            for (int i = 0; i < 64; i++) {
                if (i % 16 == 0) System.out.printf("    0x%02X: ", i);
                System.out.printf("%02X ", decoded[i] & 0xFF);
                if ((i + 1) % 16 == 0) System.out.println();
            }

            // Try to interpret the first few fields
            System.out.println("\nInterpreting first fields (should be CRCs and sizes):");
            long seqCrc = readLE64(decoded, 0);
            long seqKey = readLE64(decoded, 8);
            long comprCrc = readLE64(decoded, 16);
            int comprLen = readLE32(decoded, 24);
            int len2 = readLE32(decoded, 28);

            System.out.printf("  sequence_crc:   0x%016X\n", seqCrc);
            System.out.printf("  sequence_key:   0x%016X\n", seqKey);
            System.out.printf("  compr_crc:      0x%016X\n", comprCrc);
            System.out.printf("  compr_len:      %d\n", comprLen);
            System.out.printf("  len2:           %d\n\n", len2);

            // Check Dwg_R2007_Header fields
            System.out.println("Dwg_R2007_Header fields (starting at offset 32):");
            long headerSize = readLE64(decoded, 32 + 0);
            long fileSize = readLE64(decoded, 32 + 8);
            long pageMapOffset = readLE64(decoded, 32 + 56);
            long pageMapSizeComp = readLE64(decoded, 32 + 80);
            long pageMapSizeUncomp = readLE64(decoded, 32 + 88);

            System.out.printf("  header_size:         0x%X (%d)\n", headerSize, headerSize);
            System.out.printf("  file_size:           0x%X (%d)\n", fileSize, fileSize);
            System.out.printf("  pages_map_offset:    0x%X (%d)\n", pageMapOffset, pageMapOffset);
            System.out.printf("  pages_map_size_comp:   %d\n", pageMapSizeComp);
            System.out.printf("  pages_map_size_uncomp: %d\n", pageMapSizeUncomp);
        }

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

    private static int readLE32(byte[] data, int offset) {
        int v1 = data[offset] & 0xFF;
        int v2 = (data[offset + 1] & 0xFF) << 8;
        int v3 = (data[offset + 2] & 0xFF) << 16;
        int v4 = (data[offset + 3] & 0xFF) << 24;
        return v1 | v2 | v3 | v4;
    }
}
