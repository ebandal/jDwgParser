package debug;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.core.version.DwgVersion;
import java.nio.file.Paths;

/**
 * Deep analysis of R2000 Objects binary structure to understand offset calculation.
 * Focus: Find the actual pattern for object boundaries and size fields.
 */
public class AnalyzeR2000ObjectsBinary {
    public static void main(String[] args) throws Exception {
        String filePath = "samples/2000/Arc.dwg";
        System.out.println("=== R2000 Objects Binary Format Analysis ===");
        System.out.println("File: " + filePath);

        // Open file with DwgReader
        DwgDocument doc = DwgReader.defaultReader().open(Paths.get(filePath));
        DwgVersion version = doc.version();
        System.out.println("Version: " + version);

        // Get Objects section bytes directly from the file
        byte[] raw = readObjectsSection(filePath);
        if (raw == null) {
            System.out.println("ERROR: Could not read Objects section");
            return;
        }

        System.out.printf("\nObjects section size: %d bytes (0x%X)\n", raw.length, raw.length);

        // Analyze first 2000 bytes in detail
        analyzeFirstObjects(raw);

        // Find all 00 FF markers
        findAllMarkers(raw);

        // Analyze marker spacing
        analyzeMarkerSpacing(raw);
    }

    private static byte[] readObjectsSection(String filePath) throws Exception {
        // Read raw file
        java.nio.file.Files.readAllBytes(Paths.get(filePath));

        // For now, try to extract from structure
        // This is a simplified approach - we'll parse the file structure
        DwgDocument doc = DwgReader.defaultReader().open(Paths.get(filePath));

        // Access the raw bytes through the document's internal state
        // This requires looking at how DwgFileStructure works
        System.out.println("Using DwgDocument - objectMap size: " + doc.objectMap().size());

        // Try alternate approach: read raw file and find Objects section
        return readObjectsSectionFromRawFile(filePath);
    }

    private static byte[] readObjectsSectionFromRawFile(String filePath) throws Exception {
        byte[] fileData = java.nio.file.Files.readAllBytes(Paths.get(filePath));

        // R2000 structure: Header at offset 0x1800 (typically), Objects data starts after
        // For Arc.dwg: Objects data is from ~0x6D91 to ~0x8B9C (7,692 bytes)
        // This is file-specific, so we need a more general approach

        // Look for Objects section by finding structure
        // Simplified: assume Objects section is in a certain range
        // This is a temporary approach

        System.out.println("File size: " + fileData.length);

        // Try to find by looking at file structure
        // For R2000 samples, Objects section typically starts after header
        // Skip first reasonable amount and extract a portion

        if (fileData.length > 0x10000) {
            // Extract a reasonable chunk for analysis (up to 50KB)
            int startOffset = Math.min(0x6000, fileData.length / 10);
            int endOffset = Math.min(startOffset + 50000, fileData.length);

            byte[] section = new byte[endOffset - startOffset];
            System.arraycopy(fileData, startOffset, section, 0, section.length);
            return section;
        }

        return fileData;
    }

    private static void analyzeFirstObjects(byte[] raw) {
        System.out.println("\n=== First 2000 bytes detailed analysis ===");
        int limit = Math.min(2000, raw.length);

        for (int i = 0; i < limit; i += 16) {
            System.out.printf("0x%04X: ", i);

            // Hex bytes
            for (int j = 0; j < 16 && i + j < limit; j++) {
                System.out.printf("%02X ", raw[i + j] & 0xFF);
            }
            System.out.print(" | ");

            // ASCII
            for (int j = 0; j < 16 && i + j < limit; j++) {
                byte b = raw[i + j];
                if (b >= 32 && b <= 126) {
                    System.out.print((char) b);
                } else {
                    System.out.print(".");
                }
            }
            System.out.println();
        }
    }

    private static void findAllMarkers(byte[] raw) {
        System.out.println("\n=== All 00 FF markers in section ===");
        int count = 0;
        for (int i = 0; i < raw.length - 1; i++) {
            if ((raw[i] & 0xFF) == 0x00 && (raw[i + 1] & 0xFF) == 0xFF) {
                count++;
                if (count <= 20) {  // Show first 20
                    analyzeMarkerAt(raw, i, count);
                }
            }
        }
        System.out.printf("\nTotal 00 FF markers found: %d\n", count);
    }

    private static void analyzeMarkerAt(byte[] raw, int offset, int markerNum) {
        System.out.printf("\n[Marker %d @ 0x%04X]\n", markerNum, offset);

        // Show bytes around marker
        System.out.print("  Before: ");
        for (int i = Math.max(0, offset - 4); i < offset; i++) {
            System.out.printf("%02X ", raw[i] & 0xFF);
        }
        System.out.println();

        System.out.print("  Marker: 00 FF");
        if (offset + 10 < raw.length) {
            // After marker
            System.out.print(" | After: ");
            for (int i = offset + 2; i < Math.min(offset + 12, raw.length); i++) {
                System.out.printf("%02X ", raw[i] & 0xFF);
            }
        }
        System.out.println();

        // Try to read structure
        if (offset + 10 <= raw.length) {
            // Format: 00 FF [2 bytes] [size:RS at +2] ...
            int size = ((raw[offset + 3] & 0xFF) << 8) | (raw[offset + 2] & 0xFF);
            int typeCode = raw[offset + 8] & 0xFF;

            System.out.printf("  Size (at +2): %d (0x%X) - %s\n",
                size, size,
                (size > 0 && size < 100000) ? "VALID" : "INVALID");
            System.out.printf("  TypeCode (at +8): 0x%02X - %s\n",
                typeCode,
                isValidTypeCode(typeCode) ? "VALID" : "INVALID");
        }
    }

    private static boolean isValidTypeCode(int code) {
        return (code >= 0x30 && code <= 0x4F) || code == 0x14 || code == 0x15;
    }

    private static void analyzeMarkerSpacing(byte[] raw) {
        System.out.println("\n=== Marker spacing analysis ===");
        int[] markerOffsets = new int[100];
        int markerCount = 0;

        for (int i = 0; i < raw.length - 1 && markerCount < 100; i++) {
            if ((raw[i] & 0xFF) == 0x00 && (raw[i + 1] & 0xFF) == 0xFF) {
                markerOffsets[markerCount++] = i;
            }
        }

        System.out.printf("Found %d markers\n", markerCount);
        System.out.println("\nSpacing between consecutive markers:");

        for (int i = 0; i < Math.min(markerCount - 1, 20); i++) {
            int spacing = markerOffsets[i + 1] - markerOffsets[i];

            // Read size field from current marker
            int sizeField = -1;
            if (markerOffsets[i] + 4 <= raw.length) {
                sizeField = ((raw[markerOffsets[i] + 3] & 0xFF) << 8) |
                           (raw[markerOffsets[i] + 2] & 0xFF);
            }

            // Expected offset would be marker position + 2 (marker size) + sizeField
            int expectedSpacing = (sizeField > 0 && sizeField < 100000) ? sizeField + 2 : -1;

            System.out.printf("Marker %d->%d: spacing=%d bytes, sizeField=%d, expected=%d, %s\n",
                i, i + 1, spacing, sizeField, expectedSpacing,
                (expectedSpacing > 0 && expectedSpacing == spacing) ? "✓ MATCH" : "✗ MISMATCH");
        }
    }
}
