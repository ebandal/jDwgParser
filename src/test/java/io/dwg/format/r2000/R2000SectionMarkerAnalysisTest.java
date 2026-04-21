package test.java.io.dwg.format.r2000;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Analyze section markers found after HEADER to understand structure
 */
public class R2000SectionMarkerAnalysisTest {
    public static void main(String[] args) throws Exception {
        System.out.println("=".repeat(80));
        System.out.println("R2000 Section Marker Analysis");
        System.out.println("=".repeat(80));
        System.out.println();

        String filePath = "samples/example_2000.dwg";
        byte[] data = Files.readAllBytes(Paths.get(filePath));

        System.out.println("File size: " + data.length + " bytes (0x" + Integer.toHexString(data.length) + ")");
        System.out.println();

        // Find first few AcDb markers
        System.out.println("AcDb markers and their context:");
        System.out.println("-".repeat(80));

        int[] acdbOffsets = findMarkerOffsets(data, "AcDb");

        for (int idx = 0; idx < Math.min(3, acdbOffsets.length); idx++) {
            int offset = acdbOffsets[idx];
            System.out.printf("\nMarker [%d] at offset 0x%X (%d):\n", idx, offset, offset);

            // Show 64 bytes before (looking for sentinel)
            System.out.println("  64 bytes BEFORE marker (looking for 16B sentinel):");
            int startBefore = Math.max(0, offset - 64);
            dumpHex(data, startBefore, Math.min(64, offset - startBefore));

            // Show marker and 32 bytes after
            System.out.println("  Marker and 32 bytes after:");
            String marker = new String(data, offset, 4);
            System.out.printf("    Marker: '%s' (0x%02X 0x%02X 0x%02X 0x%02X)\n",
                marker, data[offset] & 0xFF, data[offset+1] & 0xFF,
                data[offset+2] & 0xFF, data[offset+3] & 0xFF);

            // Try to read as string (likely section name)
            int nameLen = 0;
            StringBuilder name = new StringBuilder();
            for (int i = offset + 4; i < Math.min(offset + 64, data.length); i++) {
                byte b = data[i];
                if (b == 0) {
                    nameLen = i - offset - 4;
                    break;
                }
                if (b >= 32 && b < 127) {
                    name.append((char)b);
                }
            }
            System.out.printf("    Section name: '%s' (length: %d bytes until null)\n", name, nameLen);

            dumpHex(data, offset, Math.min(48, data.length - offset));

            // Try to find sentinel pattern before this marker
            // R13 uses 16-byte sentinel (typically all zeros or specific pattern)
            System.out.println("  Looking for 16B sentinel before marker...");
            if (offset >= 16) {
                boolean isSentinel = true;
                for (int i = 0; i < 16; i++) {
                    if (data[offset - 16 + i] != 0) {
                        isSentinel = false;
                        break;
                    }
                }
                if (isSentinel) {
                    System.out.println("    ✓ Found 16-byte zero sentinel at offset 0x" + Integer.toHexString(offset - 16));
                } else {
                    System.out.println("    ✗ No zero sentinel at offset 0x" + Integer.toHexString(offset - 16));
                    System.out.print("    Data: ");
                    for (int i = offset - 16; i < offset; i++) {
                        System.out.printf("%02X ", data[i] & 0xFF);
                    }
                    System.out.println();
                }
            }
        }

        System.out.println();
        System.out.println("=".repeat(80));
    }

    private static int[] findMarkerOffsets(byte[] data, String marker) {
        byte[] bytes = marker.getBytes();
        java.util.List<Integer> offsets = new java.util.ArrayList<>();

        for (int i = 0; i <= data.length - bytes.length; i++) {
            boolean match = true;
            for (int j = 0; j < bytes.length; j++) {
                if (data[i + j] != bytes[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                offsets.add(i);
            }
        }

        int[] result = new int[offsets.size()];
        for (int i = 0; i < offsets.size(); i++) {
            result[i] = offsets.get(i);
        }
        return result;
    }

    private static void dumpHex(byte[] data, int offset, int length) {
        for (int i = 0; i < length; i += 16) {
            System.out.printf("    0x%04X: ", offset + i);
            for (int j = 0; j < 16 && i + j < length; j++) {
                System.out.printf("%02X ", data[offset + i + j] & 0xFF);
            }
            // ASCII
            System.out.print("  ");
            for (int j = 0; j < 16 && i + j < length; j++) {
                byte b = data[offset + i + j];
                if (b >= 32 && b <= 126) {
                    System.out.print((char)b);
                } else {
                    System.out.print('.');
                }
            }
            System.out.println();
        }
    }
}
