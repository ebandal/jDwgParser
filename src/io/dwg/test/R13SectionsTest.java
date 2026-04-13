package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.format.r13.R13FileStructureHandler;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * R13 DWG 섹션 추출 테스트
 */
public class R13SectionsTest {

    public static void main(String[] args) throws Exception {
        System.out.println("===== R13 Sections Extraction Test =====\n");

        String filePath = "samples/example_r13.dwg";
        byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));

        System.out.printf("File: %s\n", filePath);
        System.out.printf("File size: %d bytes (0x%X)\n\n", fileBytes.length, fileBytes.length);

        try {
            R13FileStructureHandler handler = new R13FileStructureHandler();

            // Step 1: Read header
            System.out.println("Step 1: Reading header...");
            BitInput input = new ByteBufferBitInput(fileBytes);
            var header = handler.readHeader(input);
            System.out.println("✓ Header read successfully\n");

            // Step 2: Read sections
            System.out.println("Step 2: Reading sections...");
            input = new ByteBufferBitInput(fileBytes);
            handler.readHeader(input);  // Read header again to position input correctly

            var sections = handler.readSections(input, header);

            System.out.println("─────────────────────────────────────");
            System.out.println("Extracted Sections:");
            System.out.println("─────────────────────────────────────");

            if (sections.isEmpty()) {
                System.out.println("No sections extracted");
            } else {
                for (String sectionName : sections.keySet()) {
                    var sectionData = sections.get(sectionName);
                    byte[] rawBytes = sectionData.rawBytes();

                    System.out.printf("\n[%s]\n", sectionName);
                    System.out.printf("  Size: %d bytes (0x%X)\n", rawBytes.length, rawBytes.length);

                    if (rawBytes.length > 0) {
                        System.out.print("  First 64 bytes: ");
                        for (int i = 0; i < Math.min(64, rawBytes.length); i++) {
                            System.out.printf("%02X ", rawBytes[i] & 0xFF);
                            if ((i + 1) % 16 == 0) System.out.print("\n                 ");
                        }
                        System.out.println();

                        // Check if section has CRC at the end (2 bytes)
                        if (rawBytes.length >= 2) {
                            int crc = (rawBytes[rawBytes.length - 2] & 0xFF) |
                                     ((rawBytes[rawBytes.length - 1] & 0xFF) << 8);
                            System.out.printf("  Last 2 bytes (CRC?): 0x%04X\n", crc);
                        }
                    }
                }
            }

            System.out.println("\n─────────────────────────────────────");
            System.out.println("Summary:");
            System.out.println("─────────────────────────────────────");
            System.out.printf("✓ Successfully extracted %d sections\n", sections.size());
            long totalSize = sections.values().stream()
                .mapToLong(s -> s.rawBytes().length)
                .sum();
            System.out.printf("✓ Total section data: %d bytes\n", totalSize);

        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
