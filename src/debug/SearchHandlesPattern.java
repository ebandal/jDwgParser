package debug;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Search for Handles page patterns in R2000 Objects section
 */
public class SearchHandlesPattern {
    public static void main(String[] args) throws Exception {
        Path testFile = Paths.get("samples/2000/Arc.dwg");
        byte[] fileData = Files.readAllBytes(testFile);
        byte[] objectsData = new byte[0x6B8B - 0x60];
        System.arraycopy(fileData, 0x60, objectsData, 0, objectsData.length);

        System.out.println("=== Searching for Handles Patterns in R2000 Objects ===\n");

        // Handles pages typically:
        // 1. Start with RS_BE page size (big-endian, 2 bytes, value 256-2040)
        // 2. Followed by handle-offset pairs
        // 3. End with RS_BE CRC

        System.out.println("Looking for big-endian shorts in range 256-2040 (potential page sizes):\n");

        int found = 0;
        for (int i = 0; i < objectsData.length - 1; i++) {
            int b0 = objectsData[i] & 0xFF;
            int b1 = objectsData[i + 1] & 0xFF;
            int bigEndian = (b0 << 8) | b1;

            if (bigEndian >= 256 && bigEndian <= 2040) {
                found++;
                if (found <= 30) {
                    System.out.printf("Offset 0x%04X: 0x%02X%02X = %d (potential page size)\n", i, b0, b1, bigEndian);
                }
            }
        }

        System.out.printf("\nTotal potential page size markers: %d\n", found);

        // Also look for common Handles structure patterns
        System.out.println("\nLooking for Handle entry patterns (UMC handle_delta):\n");

        // Handle delta 0 marks end of page
        int zeroDeltas = 0;
        for (int i = 0; i < objectsData.length; i++) {
            if ((objectsData[i] & 0xFF) == 0x00 && i > 0) {
                // This could be end of a handle entry or end of page
                if ((objectsData[i-1] & 0x80) == 0) {
                    // Previous byte wasn't continuation, so this might be a zero value
                    zeroDeltas++;
                }
            }
        }
        System.out.printf("Potential UMC zero values: %d\n", zeroDeltas);
    }
}
