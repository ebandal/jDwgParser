package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.util.ByteUtils;
import io.dwg.core.util.Lz77Decompressor;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Debug PageMap parsing logic.
 */
public class DebugR2007PageMapParsing {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Debugging R2007 PageMap Parsing");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] data = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));
        BitInput input = new ByteBufferBitInput(data);

        // Read PageMap header and decompress
        input.seek(0xC80 * 8);
        int type = input.readRawShort();
        long decompressedSize = input.readRawLong() & 0xFFFFFFFFL;
        long compressedSize = input.readRawLong() & 0xFFFFFFFFL;
        input.readRawLong(); // skip checksum

        // Read compressed data
        byte[] compressed = new byte[(int) Math.min(compressedSize, 0x10000)];
        int read = 0;
        while (read < compressed.length && !input.isEof()) {
            compressed[read++] = (byte) input.readRawChar();
        }

        // Decompress
        Lz77Decompressor lz77 = new Lz77Decompressor();
        byte[] decompressed = lz77.decompress(compressed, (int) decompressedSize);

        System.out.printf("Decompressed PageMap: %d bytes\n\n", decompressed.length);

        // Parse PageMap entries
        System.out.println("PageMap entries (pageId, size):");
        int pos = 0;
        long runningOffset = 0x480;
        int entryCount = 0;
        int nonZeroCount = 0;

        while (pos + 8 <= decompressed.length && entryCount < 50) {
            long pageId = ByteUtils.readLE32(decompressed, pos);
            pos += 4;
            long size = ByteUtils.readLE32(decompressed, pos);
            pos += 4;

            if (pageId > 0) {
                nonZeroCount++;
                System.out.printf("  [%d] pageId=0x%X size=%d offset=0x%X\n",
                    entryCount, pageId, size, runningOffset);
            } else if (entryCount < 10) {
                System.out.printf("  [%d] pageId=0x%X (skipped) size=%d\n",
                    entryCount, pageId, size);
            }

            runningOffset += size;
            entryCount++;
        }

        System.out.printf("\nTotal entries scanned: %d\n", entryCount);
        System.out.printf("Non-zero pageIds found: %d\n", nonZeroCount);

        if (nonZeroCount == 0) {
            System.out.println("\n❌ PROBLEM: No valid pageIds found!");
            System.out.println("This explains why pageMap.offsetForPage(0) returns null");
            System.out.println("sectionMapId=0 cannot find an offset, so readSections() returns empty");
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
    }
}
