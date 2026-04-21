package debug;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.format.r2000.R2000FileStructureHandler;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class AnalyzeR2000ObjectsStructure {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("samples/2000/Arc.dwg"));

        BitInput input = new ByteBufferBitInput(data);
        R2000FileStructureHandler handler = new R2000FileStructureHandler();
        FileHeaderFields header = handler.readHeader(input);

        // Read sections to get Objects data
        input = new ByteBufferBitInput(data);
        Map<String, SectionInputStream> sections = handler.readSections(input, header);

        SectionInputStream objectsSection = sections.get("AcDb:AcDbObjects");
        if (objectsSection == null) {
            System.out.println("Objects section not found!");
            return;
        }

        byte[] objectsData = objectsSection.rawBytes();
        System.out.println("=== R2000 Objects Section Analysis ===\n");
        System.out.printf("Total size: 0x%X (%d bytes)\n\n", objectsData.length, objectsData.length);

        // Look for class/handle/object signatures
        System.out.println("【 Looking for Known Patterns 】\n");

        // 1. Text strings (null-terminated)
        System.out.println("1. Null-terminated Strings (first 1KB):");
        int stringCount = 0;
        StringBuilder currentString = new StringBuilder();
        for (int i = 0; i < Math.min(1024, objectsData.length) && stringCount < 20; i++) {
            byte b = objectsData[i];
            if (b == 0) {
                if (currentString.length() > 3) {
                    String str = currentString.toString();
                    System.out.printf("  @ 0x%X: \"%s\"\n", i - currentString.length(), str);
                    stringCount++;
                }
                currentString = new StringBuilder();
            } else if (b >= 32 && b < 127) {
                currentString.append((char)b);
            }
        }

        // 2. Hex dump of first section
        System.out.println("\n2. First 256 bytes (hex):");
        for (int i = 0; i < Math.min(256, objectsData.length); i += 16) {
            System.out.printf("  0x%04X: ", i);
            for (int j = 0; j < 16 && i + j < objectsData.length; j++) {
                System.out.printf("%02X ", objectsData[i + j] & 0xFF);
            }
            System.out.print(" | ");
            for (int j = 0; j < 16 && i + j < objectsData.length; j++) {
                byte b = objectsData[i + j];
                if (b >= 32 && b < 127) {
                    System.out.print((char)b);
                } else {
                    System.out.print(".");
                }
            }
            System.out.println();
        }

        // 3. Look for class definition markers (AcDbXxx patterns)
        System.out.println("\n3. Looking for AcDb* class markers:");
        int markerCount = 0;
        for (int i = 0; i < objectsData.length - 5 && markerCount < 30; i++) {
            if (objectsData[i] == 'A' && objectsData[i+1] == 'c' && objectsData[i+2] == 'D' && objectsData[i+3] == 'b') {
                // Found AcDb marker, extract string
                StringBuilder sb = new StringBuilder();
                for (int j = i; j < objectsData.length && j < i + 50; j++) {
                    byte b = objectsData[j];
                    if (b == 0) break;
                    if (b >= 32 && b < 127) {
                        sb.append((char)b);
                    }
                }
                if (sb.length() > 0) {
                    System.out.printf("  @ 0x%X: %s\n", i, sb);
                    markerCount++;
                }
                i += sb.length();
            }
        }

        // 4. Statistics
        System.out.println("\n4. Byte Distribution:");
        int[] histogram = new int[256];
        for (byte b : objectsData) {
            histogram[b & 0xFF]++;
        }

        System.out.println("  Most common bytes:");
        for (int i = 0; i < 256; i++) {
            if (histogram[i] > 100) {
                System.out.printf("    0x%02X: %d occurrences (%.1f%%)\n",
                    i, histogram[i], (100.0 * histogram[i] / objectsData.length));
            }
        }
    }
}
