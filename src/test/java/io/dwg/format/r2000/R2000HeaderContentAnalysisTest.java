package io.dwg.format.r2000;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.format.common.DwgFileStructureHandler;
import io.dwg.format.common.DwgFileStructureHandlerFactory;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.core.version.DwgVersion;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Analyze HEADER section content to understand where other sections might be
 */
public class R2000HeaderContentAnalysisTest {
    public static void main(String[] args) throws Exception {
        System.out.println("=".repeat(80));
        System.out.println("R2000 HEADER Section Content Analysis");
        System.out.println("=".repeat(80));
        System.out.println();

        String filePath = "samples/example_2000.dwg";
        byte[] data = Files.readAllBytes(Paths.get(filePath));

        DwgVersion version = DwgVersion.R2000;
        DwgFileStructureHandler handler = DwgFileStructureHandlerFactory.forVersion(version);

        BitInput input = new ByteBufferBitInput(data);
        FileHeaderFields headerFields = handler.readHeader(input);

        System.out.println("Header fields summary:");
        System.out.println("  Preview offset: 0x" + Long.toHexString(headerFields.previewOffset()));
        System.out.println("  Section offsets: " + headerFields.sectionOffsets().keySet());
        System.out.println();

        // Read sections
        input = new ByteBufferBitInput(data);
        Map<String, SectionInputStream> sections = handler.readSections(input, headerFields);

        System.out.println("Sections found: " + sections.size());
        for (String name : sections.keySet()) {
            SectionInputStream stream = sections.get(name);
            System.out.printf("  Section '%s': %d bytes\n", name, stream.size());

            // Get raw data (use reflection to access rawData field)
            byte[] rawData = new byte[0];
            try {
                java.lang.reflect.Field field = stream.getClass().getDeclaredField("rawData");
                field.setAccessible(true);
                rawData = (byte[]) field.get(stream);
            } catch (Exception e) {
                System.out.println("    Error accessing raw data: " + e.getMessage());
                continue;
            }

            int limit = Math.min(256, rawData.length);
            System.out.println("    First " + limit + " bytes (hex):");
            for (int i = 0; i < limit; i += 16) {
                System.out.printf("    0x%04X: ", i);
                for (int j = 0; j < 16 && i + j < limit; j++) {
                    System.out.printf("%02X ", rawData[i + j] & 0xFF);
                }
                // Try ASCII
                System.out.print("  ");
                for (int j = 0; j < 16 && i + j < limit; j++) {
                    byte b = rawData[i + j];
                    if (b >= 32 && b <= 126) {
                        System.out.print((char)b);
                    } else {
                        System.out.print('.');
                    }
                }
                System.out.println();
            }
            System.out.println();
        }

        System.out.println("File size: " + data.length + " bytes (0x" + Integer.toHexString(data.length) + ")");
        System.out.println();
        System.out.println("Searching for known section markers...");

        // Search for AcDb section markers
        searchForMarker(data, "AcDb");
        searchForMarker(data, "ACDB");

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
