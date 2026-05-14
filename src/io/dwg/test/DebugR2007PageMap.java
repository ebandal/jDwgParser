package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.util.ByteUtils;
import io.dwg.core.util.Lz77Decompressor;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Debug R2007 PageMap reading.
 */
public class DebugR2007PageMap {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Debugging R2007 PageMap Reading");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] data = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));
        BitInput input = new ByteBufferBitInput(data);

        // PageMapOffset is at 0xC80
        long pageMapOffset = 0xC80;
        System.out.printf("PageMapOffset: 0x%X (%d)\n\n", pageMapOffset, pageMapOffset);

        // Seek to page map (convert to bit offset)
        input.seek(pageMapOffset * 8);

        System.out.println("Reading PageMap header...");

        // Page map header format (like in R2007FileStructureHandler):
        // type(RS=2), decompressedSize(RL=4), compressedSize(RL=4), checksum(RL=4)
        int type = input.readRawShort();
        long decompressedSize = input.readRawLong() & 0xFFFFFFFFL;
        long compressedSize = input.readRawLong() & 0xFFFFFFFFL;
        long checksum = input.readRawLong();

        System.out.printf("  Type: 0x%X\n", type);
        System.out.printf("  DecompressedSize: %d (0x%X)\n", decompressedSize, decompressedSize);
        System.out.printf("  CompressedSize: %d (0x%X)\n", compressedSize, compressedSize);
        System.out.printf("  Checksum: 0x%X\n\n", checksum);

        // Sanity check
        if (compressedSize > 0x1000000) {
            System.out.println("❌ CompressedSize is unreasonably large - returning empty PageMap");
            return;
        }

        // Read compressed data
        System.out.printf("Reading %d bytes of compressed data...\n", Math.min(compressedSize, 0x10000));
        byte[] compressed = new byte[(int) Math.min(compressedSize, 0x10000)];
        int read = 0;
        while (read < compressed.length && !input.isEof()) {
            compressed[read++] = (byte) input.readRawChar();
        }

        System.out.printf("Actually read: %d bytes\n\n", read);

        // Try decompression
        System.out.println("Attempting LZ77 decompression...");
        try {
            Lz77Decompressor lz77 = new Lz77Decompressor();
            byte[] decompressed = lz77.decompress(compressed, (int) decompressedSize);
            System.out.printf("✓ Decompression successful: %d bytes\n", decompressed.length);
            
            System.out.printf("\nFirst 256 bytes of decompressed PageMap data:\n");
            for (int i = 0; i < Math.min(256, decompressed.length); i += 16) {
                System.out.printf("0x%02X: ", i);
                for (int j = 0; j < 16 && i + j < decompressed.length; j++) {
                    System.out.printf("%02X ", decompressed[i + j] & 0xFF);
                }
                System.out.println();
            }
        } catch (Exception e) {
            System.out.printf("❌ Decompression failed: %s\n", e.getMessage());
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
    }
}
