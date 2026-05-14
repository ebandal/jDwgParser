package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Debug what's being passed to the LZ77 decompressor for PageMap.
 */
public class DebugR2007PageMapInput {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Debugging R2007 PageMap Input Data");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] data = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));
        BitInput input = new ByteBufferBitInput(data);

        // Read header to get pageMapOffset
        input.seek(0);
        String signature = "";
        for (int i = 0; i < 6; i++) {
            signature += (char) input.readRawChar();
        }
        System.out.printf("File signature: %s\n", signature);

        // Skip to byte 0x20 where pageMapOffset should be (in R2007 header)
        input.seek(0x20 * 8);
        long pageMapOffset = input.readRawLong();
        System.out.printf("PageMapOffset from header: 0x%X (%d)\n\n", pageMapOffset, pageMapOffset);

        // Read PageMap header
        System.out.println("PageMap Header:");
        input.seek(pageMapOffset * 8);

        int type = input.readRawShort();
        long decompressedSize = input.readRawLong() & 0xFFFFFFFFL;
        long compressedSize = input.readRawLong() & 0xFFFFFFFFL;
        long checksum = input.readRawLong();

        System.out.printf("  Type: 0x%04X\n", type);
        System.out.printf("  DecompressedSize: %d (0x%X)\n", decompressedSize, decompressedSize);
        System.out.printf("  CompressedSize: %d (0x%X)\n", compressedSize, compressedSize);
        System.out.printf("  Checksum: 0x%X\n\n", checksum);

        // Show first 64 bytes of compressed data
        byte[] compressed = new byte[Math.min(64, (int) compressedSize)];
        for (int i = 0; i < compressed.length && !input.isEof(); i++) {
            compressed[i] = (byte) input.readRawChar();
        }

        System.out.println("First 64 bytes of compressed data:");
        for (int i = 0; i < compressed.length; i++) {
            if (i % 16 == 0) System.out.printf("  0x%04X: ", i);
            System.out.printf("%02X ", compressed[i] & 0xFF);
            if (i % 16 == 15) System.out.println();
        }
        System.out.println();

        // Check if decompressed size is sensible
        if (decompressedSize > 1000000) {
            System.out.println("⚠️ WARNING: Decompressed size is very large!");
        }

        if (decompressedSize < compressedSize) {
            System.out.println("⚠️ WARNING: Decompressed size < compressed size!");
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
    }
}
