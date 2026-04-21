package debug;

import java.nio.file.Files;
import java.nio.file.Paths;

public class InvestigateR2000FullStructure {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("samples/2000/Arc.dwg"));

        System.out.println("=== R2000 Full File Structure Analysis ===\n");

        // 1. Read header to find locators
        System.out.println("【 Section Locators (from file header) 】");
        int sectionCount = data[21] & 0xFF;
        System.out.printf("Section count: %d\n\n", sectionCount);

        for (int i = 0; i < Math.min(sectionCount, 10); i++) {
            int pos = 22 + (i * 12);
            int number = readLE32(data, pos);
            int address = readLE32(data, pos + 4);
            int size = readLE32(data, pos + 8);

            System.out.printf("Locator %d: number=%d, address=0x%X, size=0x%X\n",
                i, number, address, size);

            // Check if address is reasonable
            if (address > 0 && address < data.length) {
                System.out.printf("  Data @ 0x%X: ", address);
                for (int j = 0; j < 16 && address + j < data.length; j++) {
                    System.out.printf("%02X ", data[address + j] & 0xFF);
                }
                System.out.println();

                // Check for sentinels
                if (address + 16 <= data.length) {
                    byte[] sentinel = new byte[16];
                    System.arraycopy(data, address, sentinel, 0, 16);
                    if (isSentinel(sentinel)) {
                        System.out.println("    → Looks like a sentinel marker!");
                    }
                }
            } else {
                System.out.println("  [Address out of bounds]");
            }
        }

        // 2. Look for section boundaries between 0x60 and 0x6B8B
        System.out.println("\n【 Searching for embedded section boundaries 】");
        System.out.println("\nLooking for sentinel patterns between 0x60 and 0x6B8B...");

        int objectsStart = 0x60;
        int headerStart = 0x6B8B;

        // Common sentinels to look for
        byte[][] sentinels = {
            // Classes START
            {(byte)0x8D, (byte)0xA1, (byte)0xC4, (byte)0xB8, (byte)0xC4, (byte)0xA9,
             (byte)0xF8, (byte)0xC5, (byte)0xC0, (byte)0xDC, (byte)0xF4, (byte)0x5F,
             (byte)0xE7, (byte)0xCF, (byte)0xB6, (byte)0x8A},
            // Classes END
            {(byte)0x72, (byte)0x5E, (byte)0x3B, (byte)0x47, (byte)0x3B, (byte)0x56,
             (byte)0x07, (byte)0x3A, (byte)0x3F, (byte)0x23, (byte)0x0B, (byte)0xA0,
             (byte)0x18, (byte)0x30, (byte)0x49, (byte)0x75},
            // All zeros (generic sentinel)
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
        };

        String[] sentinelNames = {"Classes START", "Classes END", "All zeros"};

        for (int s = 0; s < sentinels.length; s++) {
            System.out.printf("\nSearching for %s sentinel...\n", sentinelNames[s]);
            byte[] sentinel = sentinels[s];
            int count = 0;

            for (int i = objectsStart; i <= headerStart - sentinel.length && count < 5; i++) {
                boolean found = true;
                for (int j = 0; j < sentinel.length; j++) {
                    if (data[i + j] != sentinel[j]) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    System.out.printf("  Found at 0x%X (offset 0x%X from Objects start)\n",
                        i, i - objectsStart);
                    count++;
                }
            }

            if (count == 0) {
                System.out.println("  Not found");
            }
        }

        // 3. Look for recognizable patterns
        System.out.println("\n【 Pattern Analysis 】");

        // Look for "AcDb" strings
        System.out.println("\nSearching for 'AcDb' strings in Objects section...");
        int acdbCount = 0;
        for (int i = objectsStart; i <= headerStart - 4; i++) {
            if (data[i] == 'A' && data[i+1] == 'c' && data[i+2] == 'D' && data[i+3] == 'b') {
                // Found AcDb, extract the full string
                StringBuilder sb = new StringBuilder();
                for (int j = i; j < headerStart && j < i + 50; j++) {
                    byte b = data[j];
                    if (b == 0) break;
                    if (b >= 32 && b < 127) {
                        sb.append((char)b);
                    }
                }
                if (sb.length() > 0 && acdbCount < 5) {
                    System.out.printf("  @ 0x%X: %s\n", i, sb);
                    acdbCount++;
                }
                if (acdbCount >= 5) break;
            }
        }

        // 4. Byte distribution in Objects section
        System.out.println("\n【 Objects Section Statistics 】");
        int[] histogram = new int[256];
        for (int i = objectsStart; i < headerStart; i++) {
            histogram[data[i] & 0xFF]++;
        }

        System.out.println("Bytes with >1% frequency:");
        for (int i = 0; i < 256; i++) {
            if (histogram[i] > (headerStart - objectsStart) / 100) {
                System.out.printf("  0x%02X: %d (%.1f%%)\n",
                    i, histogram[i], (100.0 * histogram[i] / (headerStart - objectsStart)));
            }
        }
    }

    static boolean isSentinel(byte[] data) {
        // Check if all bytes are the same
        if (data.length == 0) return false;
        for (int i = 1; i < data.length; i++) {
            if (data[i] != data[0]) return false;
        }
        return true;
    }

    static int readLE32(byte[] data, int offset) {
        if (offset + 4 > data.length) return 0;
        return (data[offset] & 0xFF)
            | ((data[offset + 1] & 0xFF) << 8)
            | ((data[offset + 2] & 0xFF) << 16)
            | ((data[offset + 3] & 0xFF) << 24);
    }
}
