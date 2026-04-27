package io.dwg.test;

import io.dwg.core.util.ReedSolomonDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Deeply analyze the RS-decoded header to understand field layout
 */
public class DebugRSHeaderFields {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Analyzing RS-Decoded Header Structure");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        // Extract RS-encoded header
        byte[] rsEncoded = new byte[0x3d8];
        System.arraycopy(data, 0x80, rsEncoded, 0, 0x3d8);

        // Decode
        byte[] decoded = ReedSolomonDecoder.decodeR2007Data(rsEncoded);

        if (decoded == null) {
            System.out.println("❌ RS decoder failed!");
            return;
        }

        System.out.printf("✅ Decoded %d bytes (expected 717)\n\n", decoded.length);

        // Analyze first 32 bytes (metadata)
        System.out.println("Metadata (bytes 0-31):");
        System.out.println("  Format: CRC64 + Key64 + CRC64 + ComprLen32 + Len232 + reserved");

        long crc1 = readLE64(decoded, 0);
        long key = readLE64(decoded, 8);
        long crc2 = readLE64(decoded, 16);
        int comprLen = readLE32(decoded, 24);
        int len2 = readLE32(decoded, 28);

        System.out.printf("  CRC1:      0x%016X\n", crc1);
        System.out.printf("  Key:       0x%016X\n", key);
        System.out.printf("  CRC2:      0x%016X\n", crc2);
        System.out.printf("  ComprLen:  %d (0x%X) ← compression length\n", comprLen, comprLen);
        System.out.printf("  Len2:      %d ← should be 0 when compressed\n\n", len2);

        // If compressed, try to decompress
        if (comprLen > 0) {
            System.out.println("Header is compressed (compr_len > 0)");
            System.out.println("Need to decompress bytes 32 to " + (32 + comprLen) + "\n");
        } else {
            System.out.println("Header is NOT compressed (compr_len == 0)\n");
        }

        // Analyze what's at offset 32 (either raw header or compressed data)
        System.out.println("Data at offset 32 (first 64 bytes):");
        System.out.println("If uncompressed: This is Dwg_R2007_Header structure");
        System.out.println("If compressed: This is compressed data\n");

        for (int i = 0; i < 64; i += 16) {
            System.out.printf("  0x%02X: ", 32 + i);
            for (int j = 0; j < 16 && (32 + i + j) < decoded.length; j++) {
                System.out.printf("%02X ", decoded[32 + i + j] & 0xFF);
            }
            System.out.println();
        }

        // Try interpreting as raw header
        System.out.println("\nIf this is raw (uncompressed) Dwg_R2007_Header:");
        System.out.println("Offset 56 (pages_map_offset):     0x%016X");

        long field56 = readLE64(decoded, 32 + 56);
        System.out.printf("  Value: 0x%016X (%d) ← SANITY CHECK: should be < 0x1000000\n",
            field56, field56);

        if (field56 > 0 && field56 < 0x1000000) {
            System.out.println("  ✅ Looks reasonable!");
        } else {
            System.out.println("  ❌ Out of range - likely invalid or compressed data");
        }

        // Check for known patterns
        System.out.println("\nSearching for recognizable patterns in decoded data:");

        // Look for "AcDb" strings (common DWG strings)
        String decoded_str = new String(decoded, "ISO-8859-1");
        int acdb_pos = decoded_str.indexOf("AcDb");
        if (acdb_pos >= 0) {
            System.out.printf("  Found 'AcDb' at offset %d (0x%X)\n", acdb_pos, acdb_pos);
        } else {
            System.out.println("  No 'AcDb' string found");
        }

        // Check for small reasonable values (typical file offsets)
        System.out.println("\nLooking for small values that could be offsets:");
        for (int i = 32; i < 160; i += 8) {
            long val = readLE64(decoded, i);
            if (val > 0x100 && val < 0x100000) {
                System.out.printf("  Offset 0x%02X: 0x%X (%d) ← reasonable file offset\n", i, val, val);
            }
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("NEXT STEPS:");
        System.out.println("1. If compr_len > 0: Need to decompress and analyze decompressed data");
        System.out.println("2. Check if 'reasonable offsets' correspond to actual data in file");
        System.out.println("3. Compare with libredwg field layout");
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
