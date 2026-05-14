package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.format.r13.R13FileStructureHandler;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * R13 DWG 파일 파싱 테스트
 */
public class R13ParsingTest {

    public static void main(String[] args) throws Exception {
        System.out.println("===== R13 DWG Parsing Test =====\n");

        String filePath = "samples/example_r13.dwg";
        byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));

        System.out.printf("File: %s\n", filePath);
        System.out.printf("File size: %d bytes (0x%X)\n\n", fileBytes.length, fileBytes.length);

        try {
            // 파일 구조 읽기
            BitInput input = new ByteBufferBitInput(fileBytes);
            R13FileStructureHandler handler = new R13FileStructureHandler();
            var header = handler.readHeader(input);

            System.out.println("✓ Header parsing successful\n");
            System.out.println("─────────────────────────────────────");
            System.out.println("Parsed Header Fields:");
            System.out.println("─────────────────────────────────────");
            System.out.printf("Version: %s\n", header.version());
            System.out.printf("Maintenance version: %d\n", header.maintenanceVersion());
            System.out.printf("Code page: %d (0x%04X)\n", header.codePage(), header.codePage());
            System.out.printf("Preview offset: 0x%X\n", header.previewOffset());
            System.out.printf("Section count: %d\n",
                header.sectionOffsets() != null ? header.sectionOffsets().size() : 0);

            if (header.sectionOffsets() != null && !header.sectionOffsets().isEmpty()) {
                System.out.println("\nSection Offsets:");
                for (String sectionName : header.sectionOffsets().keySet()) {
                    long offset = header.sectionOffsets().get(sectionName);
                    System.out.printf("  %s: 0x%X\n", sectionName, offset);
                }
            }

            System.out.println("\n─────────────────────────────────────");
            System.out.println("Sections Extraction:");
            System.out.println("─────────────────────────────────────");

            // Reset input for section reading
            input = new ByteBufferBitInput(fileBytes);
            handler.readHeader(input);  // Skip header again

            var sections = handler.readSections(input, header);
            System.out.printf("✓ Extracted %d sections\n\n", sections.size());

            for (String sectionName : sections.keySet()) {
                var sectionData = sections.get(sectionName);
                System.out.printf("Section '%s':\n", sectionName);
                System.out.printf("  Size: %d bytes (0x%X)\n",
                    sectionData.rawBytes().length, sectionData.rawBytes().length);
                if (sectionData.rawBytes().length > 0) {
                    System.out.print("  First 32 bytes: ");
                    byte[] raw = sectionData.rawBytes();
                    for (int i = 0; i < Math.min(32, raw.length); i++) {
                        System.out.printf("%02X ", raw[i] & 0xFF);
                    }
                    System.out.println();
                }
            }

        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
