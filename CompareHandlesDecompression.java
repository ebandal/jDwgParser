import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.BitInput;
import io.dwg.format.r2007.R2007FileStructureHandler;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.core.io.SectionInputStream;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Compare decompressed Handles sections between working and broken R2007 files
 */
public class CompareHandlesDecompression {
    public static void main(String[] args) throws Exception {
        String workingFile = "samples/2007/Constraints.dwg";
        String brokenFile = "samples/2007/Arc.dwg";

        System.out.println("=== Comparing Handles Decompression ===\n");

        // Extract from working file
        System.out.println("1. Extracting from WORKING file: " + workingFile);
        byte[] workingHandles = extractHandles(workingFile);
        System.out.printf("   Size: %d bytes\n", workingHandles != null ? workingHandles.length : 0);
        if (workingHandles != null && workingHandles.length > 0) {
            dumpHex("   First 256 bytes:", workingHandles, 0, 256);
        }

        System.out.println("\n2. Extracting from BROKEN file: " + brokenFile);
        byte[] brokenHandles = extractHandles(brokenFile);
        System.out.printf("   Size: %d bytes\n", brokenHandles != null ? brokenHandles.length : 0);
        if (brokenHandles != null && brokenHandles.length > 0) {
            dumpHex("   First 256 bytes:", brokenHandles, 0, 256);
        }

        // Find first difference
        if (workingHandles != null && brokenHandles != null) {
            System.out.println("\n3. Comparing decompressed data:");
            int minLen = Math.min(workingHandles.length, brokenHandles.length);
            int firstDiff = -1;

            for (int i = 0; i < minLen; i++) {
                if (workingHandles[i] != brokenHandles[i]) {
                    firstDiff = i;
                    break;
                }
            }

            if (firstDiff >= 0) {
                System.out.printf("   First difference at byte %d (0x%X)\n", firstDiff, firstDiff);
                System.out.printf("   Working:  0x%02X\n", workingHandles[firstDiff] & 0xFF);
                System.out.printf("   Broken:   0x%02X\n", brokenHandles[firstDiff] & 0xFF);

                // Show context
                int start = Math.max(0, firstDiff - 10);
                int end = Math.min(minLen, firstDiff + 20);
                System.out.println("\n   Context (working):");
                dumpHex("   ", workingHandles, start, end);
                System.out.println("\n   Context (broken):");
                dumpHex("   ", brokenHandles, start, end);
            } else if (workingHandles.length != brokenHandles.length) {
                System.out.printf("   Data identical up to byte %d (shorter file ends)\n", minLen);
                System.out.printf("   Working length: %d, Broken length: %d\n",
                    workingHandles.length, brokenHandles.length);
            } else {
                System.out.println("   ✓ Decompressed data is IDENTICAL!");
            }
        }
    }

    private static byte[] extractHandles(String filePath) throws Exception {
        try {
            byte[] fileData = Files.readAllBytes(Paths.get(filePath));
            BitInput input = new ByteBufferBitInput(java.nio.ByteBuffer.wrap(fileData));

            R2007FileStructureHandler handler = new R2007FileStructureHandler();
            FileHeaderFields header = handler.readHeader(input);

            input = new ByteBufferBitInput(java.nio.ByteBuffer.wrap(fileData));
            Map<String, SectionInputStream> sections = handler.readSections(input, header);

            if (sections.containsKey("AcDb:Handles")) {
                return sections.get("AcDb:Handles").data();
            }
            return null;
        } catch (Exception e) {
            System.err.println("   ERROR: " + e.getMessage());
            return null;
        }
    }

    private static void dumpHex(String label, byte[] data, int start, int end) {
        System.out.println(label);
        int len = Math.min(end, data.length);
        for (int i = start; i < len; i += 16) {
            System.out.printf("   %06X: ", i);
            for (int j = i; j < Math.min(i + 16, len); j++) {
                System.out.printf("%02X ", data[j] & 0xFF);
            }
            System.out.println();
        }
    }
}
