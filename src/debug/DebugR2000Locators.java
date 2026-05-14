package debug;

import java.nio.file.Files;
import java.nio.file.Paths;

public class DebugR2000Locators {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("samples/2000/Arc.dwg"));

        // Skip R2000 header structure to get to section locators
        // AC1015 (6) + Reserved (6) + RC (1) + RL (4) + RC (1) + RC (1) + RS (2) + RC (1) = 22 bytes
        int locatorStartPos = 22;

        System.out.println("=== R2000 Section Locators (Raw) ===\n");

        // Read section count at position 22 (the RC field for section count)
        int sectionCount = data[21] & 0xFF;
        System.out.printf("Section count: %d\n\n", sectionCount);

        // Each locator is RL RL RL (12 bytes)
        // Format: number (RL), address (RL), size (RL)
        for (int i = 0; i < Math.min(sectionCount, 10); i++) {
            int pos = locatorStartPos + (i * 12);
            int number = readLE32(data, pos);
            int address = readLE32(data, pos + 4);
            int size = readLE32(data, pos + 8);

            System.out.printf("Locator %d @ offset 0x%X:\n", i, pos);
            System.out.printf("  number: %d (0x%X)\n", number, number);
            System.out.printf("  address: 0x%X (%d)\n", address, address);
            System.out.printf("  size: 0x%X (%d)\n", size, size);

            if (address < data.length) {
                // Show what's at this address
                System.out.print("  data @ address: ");
                for (int j = 0; j < 16 && address + j < data.length; j++) {
                    System.out.printf("%02X ", data[address + j] & 0xFF);
                }
                System.out.println();
            } else {
                System.out.println("  [ADDRESS OUT OF BOUNDS]");
            }
            System.out.println();
        }
    }

    static int readLE32(byte[] data, int offset) {
        return (data[offset] & 0xFF)
            | ((data[offset + 1] & 0xFF) << 8)
            | ((data[offset + 2] & 0xFF) << 16)
            | ((data[offset + 3] & 0xFF) << 24);
    }
}
