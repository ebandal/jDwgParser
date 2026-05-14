package debug;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Simple hex dump tool for R2000 files.
 * Shows the binary structure to understand Objects format.
 */
public class HexDumpR2000 {
    public static void main(String[] args) throws Exception {
        String filePath = "samples/2000/Arc.dwg";
        byte[] data = Files.readAllBytes(Paths.get(filePath));

        System.out.println("=== R2000 File Hex Dump (Arc.dwg) ===");
        System.out.printf("Total file size: %d bytes (0x%X)\n\n", data.length, data.length);

        // Show from offset 0x6D00 to 0x9000 (Objects section area)
        int startOffset = 0x6D00;
        int endOffset = Math.min(0x9000, data.length);

        System.out.printf("Showing bytes 0x%04X to 0x%04X:\n", startOffset, endOffset);
        System.out.println("Offset | Hex                              | ASCII");
        System.out.println("-------+----------------------------------+------------------");

        for (int i = startOffset; i < endOffset; i += 16) {
            System.out.printf("0x%04X | ", i);

            // Hex bytes
            StringBuilder hex = new StringBuilder();
            StringBuilder ascii = new StringBuilder();
            for (int j = 0; j < 16 && i + j < endOffset; j++) {
                byte b = data[i + j];
                hex.append(String.format("%02X ", b & 0xFF));

                if (b >= 32 && b <= 126) {
                    ascii.append((char) b);
                } else {
                    ascii.append(".");
                }
            }

            // Pad hex to fixed width
            while (hex.length() < 48) {
                hex.append(" ");
            }

            System.out.print(hex);
            System.out.println("| " + ascii);
        }

        // Now analyze for markers and patterns
        analyzeMarkers(data);
    }

    private static void analyzeMarkers(byte[] data) {
        System.out.println("\n=== Marker Analysis ===");
        System.out.println("Looking for 00 FF markers...\n");

        int count = 0;
        for (int i = 0; i < data.length - 1; i++) {
            if ((data[i] & 0xFF) == 0x00 && (data[i + 1] & 0xFF) == 0xFF) {
                count++;
                if (count <= 20) {
                    analyzeMarkerAt(data, i, count);
                }
            }
        }

        System.out.printf("\n\nTotal 00 FF markers found: %d\n", count);
    }

    private static void analyzeMarkerAt(byte[] data, int offset, int markerNum) {
        System.out.printf("[Marker #%d @ 0x%04X]\n", markerNum, offset);

        // Show 16 bytes from marker start
        System.out.print("  Bytes: ");
        for (int i = 0; i < 16 && offset + i < data.length; i++) {
            System.out.printf("%02X ", data[offset + i] & 0xFF);
        }
        System.out.println();

        // Try to parse as object header
        if (offset + 10 <= data.length) {
            // Byte 2-3: Size field (little-endian)
            int size = ((data[offset + 3] & 0xFF) << 8) | (data[offset + 2] & 0xFF);

            // Byte 8: Type code
            int typeCode = data[offset + 8] & 0xFF;

            System.out.printf("  Size (bytes 2-3): %d (0x%04X) - %s\n",
                size, size,
                isReasonableSize(size) ? "✓ VALID" : "✗ INVALID");

            System.out.printf("  TypeCode (byte 8): 0x%02X - %s\n",
                typeCode,
                isValidTypeCode(typeCode) ? "✓ VALID" : "✗ INVALID");

            // If there's a next marker, compute spacing
            int nextMarkerOffset = findNextMarker(data, offset + 2);
            if (nextMarkerOffset > 0) {
                int spacing = nextMarkerOffset - offset;
                int expectedSpacing = size + 2;  // marker (2 bytes) + size field + object data
                System.out.printf("  Spacing to next marker: %d bytes\n", spacing);
                System.out.printf("  Expected (2 + size): %d bytes - %s\n",
                    expectedSpacing,
                    spacing == expectedSpacing ? "✓ MATCH" : "✗ MISMATCH");
            }
        }
        System.out.println();
    }

    private static int findNextMarker(byte[] data, int startOffset) {
        for (int i = startOffset; i < data.length - 1; i++) {
            if ((data[i] & 0xFF) == 0x00 && (data[i + 1] & 0xFF) == 0xFF) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isValidTypeCode(int code) {
        return (code >= 0x30 && code <= 0x4F) || code == 0x14 || code == 0x15;
    }

    private static boolean isReasonableSize(int size) {
        return size > 0 && size < 100000;
    }
}
