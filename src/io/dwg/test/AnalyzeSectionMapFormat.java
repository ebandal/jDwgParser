package io.dwg.test;

import io.dwg.core.util.ByteUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Analyze what data we're actually reading as SectionMap
 */
public class AnalyzeSectionMapFormat {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Analyzing SectionMap Data at Offset 0x480");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        int offset = 0x480;

        // First 200 bytes
        System.out.println("Raw bytes at 0x480 (first 200 bytes):");
        for (int i = 0; i < 200; i += 16) {
            System.out.printf("  0x%03X: ", offset + i);
            for (int j = 0; j < 16 && (offset + i + j) < data.length; j++) {
                System.out.printf("%02X ", data[offset + i + j] & 0xFF);
            }
            System.out.println();
        }

        System.out.println("\nInterpreting as SectionMap:");
        System.out.println("  Expecting: sectionCount (LE32) at offset 0");

        int sectionCount = readLE32(data, offset);
        System.out.printf("  sectionCount: %d (0x%X)\n\n", sectionCount, sectionCount);

        if (sectionCount > 100) {
            System.out.println("  ❌ Section count > 100 is suspicious!");
            System.out.println("  Trying alternative: might be reading section descriptor fields\n");
        }

        // Try to parse first descriptor
        System.out.println("First descriptor starting at offset 0x04:");
        int pos = offset + 4;

        long field1 = readLE64(data, pos);
        long field2 = readLE64(data, pos + 8);
        long field3 = readLE64(data, pos + 16);
        long field4 = readLE64(data, pos + 24);
        long field5 = readLE64(data, pos + 32);
        long field6 = readLE64(data, pos + 40);
        long field7 = readLE64(data, pos + 48);
        long field8 = readLE64(data, pos + 56);

        System.out.printf("  Field 1 (data_size):     0x%X (%d)\n", field1, field1);
        System.out.printf("  Field 2 (max_size):      0x%X (%d)\n", field2, field2);
        System.out.printf("  Field 3 (encrypted):     0x%X (%d)\n", field3, field3);
        System.out.printf("  Field 4 (hashcode):      0x%X (%d)\n", field4, field4);
        System.out.printf("  Field 5 (name_length):   0x%X (%d)\n", field5, field5);
        System.out.printf("  Field 6 (unknown):       0x%X (%d)\n", field6, field6);
        System.out.printf("  Field 7 (encoded):       0x%X (%d)\n", field7, field7);
        System.out.printf("  Field 8 (num_pages):     0x%X (%d)\n\n", field8, field8);

        // Try to read name if field5 looks reasonable
        if (field5 > 0 && field5 < 512 && field5 % 2 == 0) {
            System.out.printf("  Trying to read name (%d bytes) at offset %d:\n", field5, pos + 64);
            try {
                byte[] nameBytes = new byte[(int) field5];
                System.arraycopy(data, pos + 64, nameBytes, 0, nameBytes.length);
                String name = new String(nameBytes, StandardCharsets.UTF_16LE);
                System.out.printf("    Name: \"%s\"\n", name);
            } catch (Exception e) {
                System.out.println("    Failed to decode name");
            }
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
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
