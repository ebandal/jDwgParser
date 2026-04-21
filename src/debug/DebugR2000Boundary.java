package debug;

import java.nio.file.Files;
import java.nio.file.Paths;

public class DebugR2000Boundary {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("samples/2000/Arc.dwg"));

        int headerStart = 0x6B8B;
        int objectsStart = 0x60;

        System.out.println("=== R2000 File Boundaries ===\n");
        System.out.printf("Objects data: 0x%X to 0x%X (%d bytes)\n",
            objectsStart, headerStart - 1, headerStart - objectsStart);

        System.out.println("\nBytes just BEFORE Header section (0x6B7B-0x6B8A):");
        for (int i = 0x6B7B; i <= 0x6B8A && i < data.length; i++) {
            byte b = data[i];
            System.out.printf("  0x%X: 0x%02X (%c)\n", i, b & 0xFF,
                (b >= 32 && b < 127) ? (char)b : '.');
        }

        System.out.println("\nLooking for sentinel pattern near Header boundary...");
        int sentinelCount = 0;
        for (int i = 0x6B80; i <= 0x6B8A && i < data.length; i++) {
            // Check for 16 consecutive zeros
            boolean isSentinel = true;
            if (i + 16 <= data.length) {
                for (int j = 0; j < 16; j++) {
                    if (data[i + j] != 0) {
                        isSentinel = false;
                        break;
                    }
                }
                if (isSentinel) {
                    System.out.printf("  Sentinel found @ 0x%X\n", i);
                    sentinelCount++;
                }
            }
        }

        if (sentinelCount == 0) {
            System.out.println("  No 16-byte sentinel found near boundary");
        }

        System.out.println("\nBytes just AFTER Objects start (0x60-0x7F):");
        for (int i = 0x60; i <= 0x7F && i < data.length; i++) {
            byte b = data[i];
            System.out.printf("%02X ", b & 0xFF);
            if ((i - 0x60 + 1) % 16 == 0) System.out.println();
        }
    }
}
