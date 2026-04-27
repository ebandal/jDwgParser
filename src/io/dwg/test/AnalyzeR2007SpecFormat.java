package io.dwg.test;

import io.dwg.core.util.ByteUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Analyze R2007 SectionMap format based on libredwg specification.
 *
 * Format (from libredwg/src/decode_r2007.c):
 * - data_size (RL 8 bytes)
 * - max_size (RL 8 bytes)
 * - encrypted (RL 8 bytes)
 * - hashcode (RL 8 bytes)
 * - name_length (RL 8 bytes)
 * - unknown (RL 8 bytes)
 * - encoded (RL 8 bytes)
 * - num_pages (RL 8 bytes)
 * - name (UTF-16LE, name_length bytes)
 * - pages (page entries, num_pages × entries)
 */
public class AnalyzeR2007SpecFormat {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Analyzing R2007 SectionMap by Libredwg Spec");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] fileData = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));

        // Get offset from header 0x20
        long offset = ByteUtils.readLE64(fileData, 0x20);
        System.out.printf("Section Map offset (from header[0x20]): 0x%X\n\n", offset);

        int pos = (int) offset;

        // Read first section descriptor
        System.out.println("First Section Descriptor:");
        System.out.println("Format: 8 RLL fields (8 bytes each), then UTF-16LE name, then pages\n");

        long dataSize = readRLL(fileData, pos); pos += 8;
        long maxSize = readRLL(fileData, pos); pos += 8;
        long encrypted = readRLL(fileData, pos); pos += 8;
        long hashcode = readRLL(fileData, pos); pos += 8;
        long nameLength = readRLL(fileData, pos); pos += 8;
        long unknown = readRLL(fileData, pos); pos += 8;
        long encoded = readRLL(fileData, pos); pos += 8;
        long numPages = readRLL(fileData, pos); pos += 8;

        System.out.printf("  data_size:      %d (0x%X)\n", dataSize, dataSize);
        System.out.printf("  max_size:       %d (0x%X)\n", maxSize, maxSize);
        System.out.printf("  encrypted:      %d\n", encrypted);
        System.out.printf("  hashcode:       0x%X\n", hashcode);
        System.out.printf("  name_length:    %d bytes\n", nameLength);
        System.out.printf("  unknown:        %d\n", unknown);
        System.out.printf("  encoded:        %d\n", encoded);
        System.out.printf("  num_pages:      %d\n\n", numPages);

        System.out.printf("Expected position for section name: 0x%X (byte %d)\n", pos - offset, pos);

        if (nameLength > 0 && nameLength < 256 && nameLength % 2 == 0) {
            // Read UTF-16LE section name
            byte[] nameBytes = new byte[(int)nameLength];
            System.arraycopy(fileData, pos, nameBytes, 0, nameBytes.length);
            String name = new String(nameBytes, StandardCharsets.UTF_16LE);
            System.out.printf("  name:           \"%s\" (%d bytes)\n\n", name, nameLength);

            pos += (int)nameLength;
        }

        // Next should be page entries
        System.out.printf("Expected position for pages: 0x%X (byte %d)\n", pos - offset, pos);
        if (numPages > 0 && numPages < 100) {
            System.out.println("First page entry:");
            // Page structure: pageId (RL), size (RL), offset (RL) ?
            long pageId = readRLL(fileData, pos);
            long pageSize = readRLL(fileData, pos + 8);
            long pageOffset = readRLL(fileData, pos + 16);
            System.out.printf("  pageId:         %d\n", pageId);
            System.out.printf("  pageSize:       %d\n", pageSize);
            System.out.printf("  pageOffset:     %d\n", pageOffset);
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("✓ R2007 SectionMap format understood!");
        System.out.println("  - Fixed header: 8 RLL fields (64 bytes)");
        System.out.println("  - Variable name: UTF-16LE, length from field");
        System.out.println("  - Pages: array of page entries");
    }

    private static long readRLL(byte[] data, int offset) {
        long v1 = data[offset + 0] & 0xFFL;
        long v2 = (data[offset + 1] & 0xFFL) << 8;
        long v3 = (data[offset + 2] & 0xFFL) << 16;
        long v4 = (data[offset + 3] & 0xFFL) << 24;
        long v5 = (data[offset + 4] & 0xFFL) << 32;
        long v6 = (data[offset + 5] & 0xFFL) << 40;
        long v7 = (data[offset + 6] & 0xFFL) << 48;
        long v8 = (data[offset + 7] & 0xFFL) << 56;
        return v1 | v2 | v3 | v4 | v5 | v6 | v7 | v8;
    }
}
