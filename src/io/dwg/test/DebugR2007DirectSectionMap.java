package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.util.ByteUtils;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Check if the offset at 0x20 points directly to SectionMap, not PageMap.
 */
public class DebugR2007DirectSectionMap {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Testing Direct SectionMap at Header Offset 0x20");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] fileData = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));
        BitInput input = new ByteBufferBitInput(fileData);

        // Get offset from header 0x20
        input.seek(0x20 * 8);
        long offset = input.readRawLong();
        System.out.printf("Offset from header[0x20]: 0x%X (%d bytes)\n\n", offset, offset);

        // Read data at this offset as if it were a SectionMap
        input.seek(offset * 8);

        // Read section count
        int sectionCount = (int) ByteUtils.readLE32(fileData, (int) offset);
        System.out.printf("First 4 bytes as LE32: 0x%08X (%d)\n", sectionCount & 0xFFFFFFFFL, sectionCount);
        System.out.printf("Is this a reasonable section count? %s\n\n",
            (sectionCount > 0 && sectionCount < 20) ? "YES ✓" : "NO ✗");

        // Try to parse as SectionMap
        System.out.println("Attempting to parse SectionMap:");
        int pos = (int) offset;
        if (sectionCount > 0 && sectionCount < 20) {
            for (int i = 0; i < Math.min(sectionCount, 3); i++) {
                System.out.printf("\nSection %d:\n", i);

                if (pos + 56 > fileData.length) break;

                long dataSize = ByteUtils.readLE64(fileData, pos);  pos += 8;
                long maxDecompSize = ByteUtils.readLE64(fileData, pos);  pos += 8;
                long compressionInfo = ByteUtils.readLE64(fileData, pos);  pos += 8;
                int compressionType = (int)(compressionInfo & 0xFFFFFFFFL);

                System.out.printf("  DataSize: %d\n", dataSize);
                System.out.printf("  MaxDecompSize: %d\n", maxDecompSize);
                System.out.printf("  CompressionType: %d\n", compressionType);

                // Skip 3 reserved fields
                pos += 24;

                // Read section name (64 bytes)
                byte[] nameBytes = new byte[64];
                System.arraycopy(fileData, pos, nameBytes, 0, 64);
                String name = decodeUtf16Name(nameBytes);
                pos += 64;

                System.out.printf("  Name: %s\n", name);

                // Read page count
                if (pos + 4 <= fileData.length) {
                    int pageCount = (int) ByteUtils.readLE32(fileData, pos);
                    pos += 4;
                    System.out.printf("  PageCount: %d\n", pageCount);
                }
            }
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
    }

    private static String decodeUtf16Name(byte[] bytes) {
        int len = 0;
        while (len + 1 < bytes.length && (bytes[len] != 0 || bytes[len+1] != 0)) len += 2;
        return new String(bytes, 0, len, java.nio.charset.StandardCharsets.UTF_16LE);
    }
}
