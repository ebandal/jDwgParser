package debug;

import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.BitInput;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.format.r2000.R2000FileStructureHandler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Analyze if R2000 Objects section actually contains Objects/Classes/Handles combined
 */
public class AnalyzeR2000CombinedSection {
    public static void main(String[] args) throws Exception {
        Path testFile = Paths.get("C:\\workspace_ebandal\\jDwgParser\\samples\\2000\\Arc.dwg");

        if (!Files.exists(testFile)) {
            System.err.println("Test file not found: " + testFile);
            return;
        }

        byte[] fileData = Files.readAllBytes(testFile);
        BitInput input = new ByteBufferBitInput(fileData);

        R2000FileStructureHandler handler = new R2000FileStructureHandler();
        FileHeaderFields header = handler.readHeader(input);

        String objectsName = io.dwg.format.common.SectionType.OBJECTS.sectionName();
        Long objectsOffset = header.sectionOffsets().get(objectsName);
        Long objectsSize = header.sectionSizes().get(objectsName);

        System.out.println("=== R2000 Combined Section Analysis (Arc.dwg) ===");
        System.out.printf("Objects section: offset=0x%X, size=%d bytes\n", objectsOffset, objectsSize);

        if (objectsOffset != null && objectsSize != null && objectsSize > 0) {
            input.seek(objectsOffset * 8);

            System.out.println("\n=== Section Structure (searching for markers) ===");

            // Read entire section into memory
            byte[] sectionData = new byte[(int)Math.min(objectsSize, 10000)];
            for (int i = 0; i < sectionData.length; i++) {
                sectionData[i] = (byte) input.readRawChar();
            }

            System.out.printf("Analyzing first %d bytes of %d-byte section\n\n", sectionData.length, objectsSize);

            // Look for recognizable patterns
            System.out.println("1. Looking for 'AcDb' markers (object type markers):");
            for (int i = 0; i < Math.min(1000, sectionData.length - 3); i++) {
                if (sectionData[i] == 'A' && sectionData[i+1] == 'c' && sectionData[i+2] == 'D' && sectionData[i+3] == 'b') {
                    System.out.printf("   Found at offset 0x%X: %s\n", objectsOffset + i,
                        extractString(sectionData, i, 32));
                }
            }

            System.out.println("\n2. Hex dump of first 256 bytes:");
            for (int i = 0; i < Math.min(256, sectionData.length); i++) {
                if (i % 16 == 0) System.out.printf("0x%04X: ", objectsOffset + i);
                System.out.printf("%02X ", sectionData[i] & 0xFF);
                if ((i + 1) % 16 == 0) System.out.println();
            }

            System.out.println("\n3. Possible structure divisions:");
            // Look for likely section boundaries
            // Handles section typically starts with: RS_BE page_size (big-endian)
            for (int i = 0; i < Math.min(2000, sectionData.length - 1); i++) {
                int byte1 = sectionData[i] & 0xFF;
                int byte2 = sectionData[i+1] & 0xFF;
                int bigEndianShort = (byte1 << 8) | byte2;

                // Handles pages are typically 256-2040 bytes
                if (bigEndianShort >= 256 && bigEndianShort <= 2040) {
                    // Check if this could be a page size
                    if ((i > 100) && (i < objectsSize - bigEndianShort - 10)) {
                        System.out.printf("   Offset 0x%X: Could be page size %d (0x%04X)\n",
                            objectsOffset + i, bigEndianShort, bigEndianShort);
                    }
                }
            }

            System.out.println("\n4. Statistics:");
            int zeroCount = 0, nonZeroCount = 0;
            for (byte b : sectionData) {
                if (b == 0) zeroCount++;
                else nonZeroCount++;
            }
            System.out.printf("   Zero bytes: %d (%.1f%%)\n", zeroCount, 100.0 * zeroCount / sectionData.length);
            System.out.printf("   Non-zero bytes: %d (%.1f%%)\n", nonZeroCount, 100.0 * nonZeroCount / sectionData.length);
        }
    }

    static String extractString(byte[] data, int offset, int maxLen) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxLen && offset + i < data.length; i++) {
            byte b = data[offset + i];
            if (b >= 32 && b < 127) {
                sb.append((char) b);
            } else if (b == 0) {
                break;
            } else {
                sb.append('.');
            }
        }
        return sb.toString();
    }
}
