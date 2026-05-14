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
 * R2000 Handles section debugging - dump section locators and raw data
 */
public class DebugR2000Handles {
    public static void main(String[] args) throws Exception {
        // Use Arc.dwg as test case
        Path testFile = Paths.get("C:\\workspace_ebandal\\jDwgParser\\samples\\2000\\Arc.dwg");

        if (!Files.exists(testFile)) {
            System.err.println("Test file not found: " + testFile);
            return;
        }

        byte[] fileData = Files.readAllBytes(testFile);
        BitInput input = new ByteBufferBitInput(fileData);

        R2000FileStructureHandler handler = new R2000FileStructureHandler();
        FileHeaderFields header = handler.readHeader(input);

        System.out.println("=== R2000 Section Locators (Arc.dwg) ===");
        System.out.println("Total section locators: " + header.sectionLocators().size());

        for (Object locObj : header.sectionLocators()) {
            if (locObj instanceof io.dwg.format.r13.R13SectionLocator loc) {
                System.out.printf("  Locator %d: offset=0x%X (%d), size=%d\n",
                    loc.recordNumber(), loc.seeker(), loc.seeker(), loc.size());
            }
        }

        System.out.println("\n=== Section Offsets/Sizes ===");
        for (String sectionName : header.sectionOffsets().keySet()) {
            long offset = header.sectionOffsets().get(sectionName);
            long size = header.sectionSizes().get(sectionName);
            System.out.printf("%s: offset=0x%X size=%d bytes\n", sectionName, offset, size);
        }

        // Now read the Handles section specifically
        System.out.println("\n=== Raw Handles Section Data ===");
        String handlesName = io.dwg.format.common.SectionType.HANDLES.sectionName();
        Long handlesOffset = header.sectionOffsets().get(handlesName);
        Long handlesSize = header.sectionSizes().get(handlesName);

        if (handlesOffset != null && handlesSize != null && handlesSize > 0) {
            input.seek(handlesOffset * 8); // Convert bytes to bits

            System.out.printf("Handles offset: 0x%X, size: %d bytes\n", handlesOffset, handlesSize);

            // Read first 256 bytes and dump
            int readSize = (int) Math.min(256, handlesSize);
            byte[] handlesData = new byte[readSize];
            for (int i = 0; i < readSize; i++) {
                handlesData[i] = (byte) input.readRawChar();
            }

            // Hex dump
            System.out.println("First " + readSize + " bytes (hex):");
            for (int i = 0; i < readSize; i++) {
                if (i % 16 == 0) {
                    System.out.printf("\n0x%04X: ", i);
                }
                System.out.printf("%02X ", handlesData[i] & 0xFF);
            }
            System.out.println();

            // Try to identify structure
            System.out.println("\nStructure analysis:");
            System.out.printf("  First 2 bytes (big-endian): 0x%02X%02X = %d\n",
                handlesData[0] & 0xFF, handlesData[1] & 0xFF,
                ((handlesData[0] & 0xFF) << 8) | (handlesData[1] & 0xFF));

            // Check for patterns
            int zeroCount = 0;
            int nonZeroCount = 0;
            for (int i = 0; i < readSize; i++) {
                if (handlesData[i] == 0) zeroCount++;
                else nonZeroCount++;
            }
            System.out.printf("  Zero bytes: %d (%.1f%%)\n", zeroCount, 100.0 * zeroCount / readSize);
            System.out.printf("  Non-zero bytes: %d (%.1f%%)\n", nonZeroCount, 100.0 * nonZeroCount / readSize);
        } else {
            System.out.println("ERROR: Handles section not found or has invalid size");
        }
    }
}
