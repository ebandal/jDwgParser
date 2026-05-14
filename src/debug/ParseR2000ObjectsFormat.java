package debug;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.version.DwgVersion;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Parse R2000 Objects section format using BitStreamReader
 */
public class ParseR2000ObjectsFormat {
    public static void main(String[] args) throws Exception {
        // Read R2000 file
        Path testFile = Paths.get("C:\\workspace_ebandal\\jDwgParser\\samples\\2000\\Arc.dwg");
        byte[] fileData = Files.readAllBytes(testFile);

        // Extract Objects section (0x60 - 0x6B8B from our earlier analysis)
        byte[] objectsData = new byte[0x6B8B - 0x60];
        System.arraycopy(fileData, 0x60, objectsData, 0, objectsData.length);

        System.out.println("=== R2000 Objects Section Format Analysis ===");
        System.out.printf("Objects section size: %d bytes\n\n", objectsData.length);

        // Create BitStreamReader
        ByteBufferBitInput input = new ByteBufferBitInput(objectsData);
        BitStreamReader reader = new BitStreamReader(input, DwgVersion.R2000);

        // Try to parse as object records
        System.out.println("Attempting to parse as Objects (bit-packed format):\n");

        int objectCount = 0;
        int attemptCount = 0;
        int maxAttempts = 20;

        while (objectCount < maxAttempts && !reader.isEof()) {
            attemptCount++;
            long posBeforeBits = reader.position();
            int posBeforeBytes = (int)(posBeforeBits / 8);

            try {
                // Try to read OT (object type, 8 bits or less as UMC)
                int objType = reader.readUnsignedModularChar();

                // Try to read RL (record length, UMC - number of bits to follow)
                int recordLength = reader.readUnsignedModularChar();

                long posAfterHeader = reader.position();

                System.out.printf("Object %d @ 0x%X (bit offset %d):\n",
                    objectCount, posBeforeBytes, posBeforeBits);
                System.out.printf("  OT (type): 0x%02X\n", objType);
                System.out.printf("  RL (record len bits): %d\n", recordLength);

                // Try to skip the record data
                if (recordLength > 0 && recordLength < 100000) {
                    reader.seek(posAfterHeader + recordLength);
                    long posAfterSkip = reader.position();
                    System.out.printf("  Skipped to bit offset %d (byte 0x%X)\n", posAfterSkip, posAfterSkip / 8);
                } else {
                    System.out.printf("  Invalid record length, stopping\n");
                    break;
                }

                objectCount++;

                if (objectCount >= 3) {
                    System.out.println("  ... (stopping at 3 objects for brevity)");
                    break;
                }

            } catch (Exception e) {
                System.out.printf("Error parsing object %d: %s\n", objectCount, e.getMessage());
                break;
            }
        }

        System.out.printf("\nTotal objects parsed: %d\n", objectCount);

        // Also show raw hex for reference
        System.out.println("\n=== Raw Hex Dump (first 256 bytes) ===");
        for (int i = 0; i < Math.min(256, objectsData.length); i++) {
            if (i % 16 == 0) System.out.printf("0x%04X: ", i);
            System.out.printf("%02X ", objectsData[i] & 0xFF);
            if ((i + 1) % 16 == 0) System.out.println();
        }
    }
}
