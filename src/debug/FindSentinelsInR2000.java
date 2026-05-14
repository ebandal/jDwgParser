package debug;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Find Sentinel patterns in R2000 Objects section.
 * R13/R14 style Sentinels for Classes/Handles sections.
 */
public class FindSentinelsInR2000 {
    public static void main(String[] args) throws Exception {
        String filePath = "samples/2000/Arc.dwg";
        System.out.println("=== R2000 Objects Section - Sentinel Search ===");

        byte[] fileData = Files.readAllBytes(Paths.get(filePath));

        // Objects section: 0x5E ~ 0x6B8B (27,437 bytes)
        int objectsStart = 0x5E;
        int objectsEnd = 0x6B8B;

        byte[] objectsSection = new byte[objectsEnd - objectsStart];
        System.arraycopy(fileData, objectsStart, objectsSection, 0, objectsSection.length);

        System.out.printf("Objects section: 0x%04X ~ 0x%04X (%d bytes)\n",
            objectsStart, objectsEnd, objectsSection.length);

        // Look for Sentinel patterns
        // Sentinel = 16 bytes with specific pattern (varies by version)
        // Common sentinels: 0xAC, 0x09, 0xAD, 0x02, repeated or specific pattern
        findSentinelLikePatterns(objectsSection);

        // Also look for RS_BE values (big-endian 16-bit values that might be page sizes)
        // Page size should be around 2032-2040 (0x7F0-0x7F8)
        System.out.println("\n=== Searching for RS_BE page size values (2000-2100) ===");
        findPageSizeValues(objectsSection);
    }

    private static void findSentinelLikePatterns(byte[] data) {
        System.out.println("\n=== Searching for Sentinel-like patterns ===");
        System.out.println("Looking for 16-byte repeated patterns...\n");

        int count = 0;
        for (int i = 0; i < data.length - 16 && count < 20; i++) {
            byte[] candidate = new byte[16];
            System.arraycopy(data, i, candidate, 0, 16);

            // Check if this looks like a sentinel (not all zeros, not all same byte)
            int uniqueBytes = countUniqueBytes(candidate);
            boolean isLikelyData = uniqueBytes >= 4;  // Real data has some variety

            // Print first occurrence of each 16-byte pattern
            if (isLikelyData && i < 1000) {
                System.out.printf("Offset 0x%04X: ", i);
                for (byte b : candidate) {
                    System.out.printf("%02X ", b & 0xFF);
                }
                System.out.println();
                count++;
            }
        }
    }

    private static int countUniqueBytes(byte[] data) {
        java.util.Set<Byte> unique = new java.util.HashSet<>();
        for (byte b : data) {
            unique.add(b);
        }
        return unique.size();
    }

    private static void findPageSizeValues(byte[] data) {
        // Look for RS_BE values (big-endian short) in range 2000-2100 (0x07D0-0x0834)

        int count = 0;
        for (int i = 0; i < data.length - 1 && count < 20; i++) {
            int valueBE = ((data[i] & 0xFF) << 8) | (data[i + 1] & 0xFF);

            if (valueBE >= 2000 && valueBE <= 2100) {
                // Could be a page size
                System.out.printf("Offset 0x%04X: value=0x%04X (%d) - ", i, valueBE, valueBE);

                // Show context (bytes before and after)
                int contextSize = 8;
                System.out.print("Context: ");
                for (int j = Math.max(0, i - contextSize); j < Math.min(data.length, i + contextSize + 2); j++) {
                    if (j == i) System.out.print("[");
                    System.out.printf("%02X", data[j] & 0xFF);
                    if (j == i + 1) System.out.print("]");
                    if (j < Math.min(data.length, i + contextSize + 2) - 1) System.out.print(" ");
                }
                System.out.println();
                count++;
            }
        }

        if (count == 0) {
            System.out.println("No page sizes found in range 2000-2100");

            // Search for smaller range
            System.out.println("\nSearching for page sizes in range 1500-3000...");
            count = 0;
            for (int i = 0; i < data.length - 1 && count < 10; i++) {
                int valueBE = ((data[i] & 0xFF) << 8) | (data[i + 1] & 0xFF);

                if (valueBE >= 1500 && valueBE <= 3000) {
                    System.out.printf("Offset 0x%04X: value=0x%04X (%d)\n", i, valueBE, valueBE);
                    count++;
                }
            }
        }
    }
}
