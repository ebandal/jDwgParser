package io.dwg.format.r2000;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Dump raw section locator data to understand the exact structure
 */
public class R2000SectionLocatorDumpTest {
    public static void main(String[] args) throws Exception {
        String filePath = "samples/example_2000.dwg";

        byte[] data = Files.readAllBytes(Paths.get(filePath));
        System.out.println("File size: " + data.length + " bytes\n");

        BitInput input = new ByteBufferBitInput(data);

        // Skip to section count
        // 0x00-0x05: Version (6 bytes)
        // 0x06-0x0B: Reserved (6 bytes)
        // 0x0C: unknown_0 (1 byte)
        // 0x0D-0x10: preview address (4 bytes)
        // 0x11: dwg version (1 byte)
        // 0x12: maint version (1 byte)
        // 0x13-0x14: codepage (2 bytes)
        // 0x15: section count (1 byte)

        for (int i = 0; i < 21; i++) {
            input.readRawChar();
        }

        int sectionCount = input.readRawChar();
        System.out.println("Section count: " + sectionCount);
        System.out.println();

        System.out.println("Section Locators (raw bytes):");
        System.out.println("-".repeat(80));

        // Track byte position for reference
        int bytePosition = 22; // After section count

        for (int i = 0; i < Math.min(sectionCount, 10); i++) {
            System.out.printf("Locator [%d] at byte offset 0x%04X:\n", i, bytePosition);

            // Read 3 longs (12 bytes)
            long val1 = input.readRawLong() & 0xFFFFFFFFL;
            long val2 = input.readRawLong() & 0xFFFFFFFFL;
            long val3 = input.readRawLong() & 0xFFFFFFFFL;

            bytePosition += 12;

            System.out.printf("  [0] 0x%08X (decimal: %d)\n", val1, val1);
            System.out.printf("  [1] 0x%08X (decimal: %d)  <- likely offset\n", val2, val2);
            System.out.printf("  [2] 0x%08X (decimal: %d)  <- likely size\n", val3, val3);

            String sectionName = switch ((int)val1) {
                case 0 -> "HEADER";
                case 1 -> "CLASSES";
                case 2 -> "HANDLES";
                case 3 -> "OBJECTS";
                default -> "UNKNOWN";
            };
            System.out.printf("  Interpreted as: %s section, offset=0x%08X, size=0x%08X\n",
                              sectionName, val2, val3);
            System.out.println();
        }
    }
}
