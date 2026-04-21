package test.java.io.dwg.format.r2000;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.format.common.DwgFileStructureHandler;
import io.dwg.format.common.DwgFileStructureHandlerFactory;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.core.version.DwgVersion;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import io.dwg.core.io.SectionInputStream;

/**
 * Search for Handles and Classes section markers in R2000 object stream
 */
public class R2000HandlesSectionSearchTest {
    public static void main(String[] args) throws Exception {
        System.out.println("=".repeat(80));
        System.out.println("R2000 Handles/Classes Section Search");
        System.out.println("=".repeat(80));
        System.out.println();

        String filePath = "samples/example_2000.dwg";
        byte[] data = Files.readAllBytes(Paths.get(filePath));

        DwgVersion version = DwgVersion.R2000;
        DwgFileStructureHandler handler = DwgFileStructureHandlerFactory.forVersion(version);

        BitInput input = new ByteBufferBitInput(data);
        FileHeaderFields headerFields = handler.readHeader(input);

        // Read sections
        input = new ByteBufferBitInput(data);
        Map<String, SectionInputStream> sections = handler.readSections(input, headerFields);

        // Get objects section
        SectionInputStream objectsStream = sections.get("AcDb:AcDbObjects");
        if (objectsStream == null) {
            System.out.println("ERROR: Objects section not found!");
            return;
        }

        System.out.println("Objects section size: " + objectsStream.size() + " bytes");
        System.out.println();

        // Get raw data
        java.lang.reflect.Field field = objectsStream.getClass().getDeclaredField("rawData");
        field.setAccessible(true);
        byte[] rawData = (byte[]) field.get(objectsStream);

        // Search for known section names
        System.out.println("Searching for section markers in object stream:");
        System.out.println("-".repeat(80));

        searchForMarker(rawData, "AcDb:Handles");
        searchForMarker(rawData, "AcDb:Classes");
        searchForMarker(rawData, "Handles");
        searchForMarker(rawData, "Classes");

        // Also search for common class markers to understand structure
        System.out.println();
        System.out.println("Sampling AcDb markers:");
        int count = 0;
        for (int i = 0; i < rawData.length - 10 && count < 15; i++) {
            if (rawData[i] == 'A' && rawData[i+1] == 'c' && rawData[i+2] == 'D' && rawData[i+3] == 'b') {
                // Found AcDb marker
                StringBuilder sb = new StringBuilder("AcDb");
                for (int j = 4; j < 50 && i + j < rawData.length; j++) {
                    byte b = rawData[i + j];
                    if (b == 0) break;  // Null terminator
                    if (b >= 32 && b <= 126) {
                        sb.append((char) b);
                    }
                }
                System.out.printf("  Offset 0x%X: %s\n", i, sb);
                count++;
                i += 50;  // Skip ahead
            }
        }

        System.out.println();
        System.out.println("=".repeat(80));
    }

    private static void searchForMarker(byte[] data, String marker) {
        byte[] bytes = marker.getBytes();
        int count = 0;
        for (int i = 0; i <= data.length - bytes.length; i++) {
            boolean match = true;
            for (int j = 0; j < bytes.length; j++) {
                if (data[i + j] != bytes[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                count++;
                System.out.printf("  Found '%s' at offset 0x%X\n", marker, i);
            }
        }
        if (count == 0) {
            System.out.printf("  No occurrences of '%s' found\n", marker);
        }
    }
}
