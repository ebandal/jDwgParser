package debug;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Analyze R2000 file structure - understand where Objects section actually is.
 * Focus: Read section locators and find Objects section boundaries.
 */
public class AnalyzeR2000Structure {
    public static void main(String[] args) throws Exception {
        String filePath = "samples/2000/Arc.dwg";
        System.out.println("=== R2000 File Structure Analysis ===");
        System.out.println("File: " + filePath);

        byte[] fileData = Files.readAllBytes(Paths.get(filePath));
        System.out.printf("Total file size: %d bytes (0x%X)\n\n", fileData.length, fileData.length);

        // Parse header manually
        analyzeHeaderAndLocators(fileData);

        // Also test with DwgReader to compare
        System.out.println("\n=== DwgReader Verification ===");
        try {
            DwgDocument doc = DwgReader.defaultReader().open(Paths.get(filePath));
            System.out.printf("Version: %s\n", doc.version());
            System.out.printf("Objects parsed: %d\n", doc.objectMap().size());
        } catch (Exception e) {
            System.out.println("DwgReader error: " + e.getMessage());
        }
    }

    private static void analyzeHeaderAndLocators(byte[] data) throws Exception {
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.order(java.nio.ByteOrder.LITTLE_ENDIAN);

        // R2000 Header structure
        // 0x00-0x05: Version string "AC1015"
        byte[] versionBytes = new byte[6];
        buf.get(versionBytes);
        String version = new String(versionBytes);
        System.out.printf("Version string: %s\n", version);

        // 0x06-0x0B: Reserved (6 bytes)
        buf.position(buf.position() + 6);

        // 0x0C: unknown_0 (RC, 1 byte)
        int unknown0 = buf.get() & 0xFF;
        System.out.printf("Unknown0: 0x%02X\n", unknown0);

        // 0x0D: Preview address (RL, 4 bytes)
        int previewAddr = buf.getInt();
        System.out.printf("Preview address: 0x%X (%d bytes)\n", previewAddr, previewAddr);

        // 0x11: DWG version (RC, 1 byte)
        int dwgVer = buf.get() & 0xFF;
        System.out.printf("DWG version: 0x%02X\n", dwgVer);

        // 0x12: Maintenance version (RC, 1 byte)
        int maintVer = buf.get() & 0xFF;
        System.out.printf("Maintenance version: %d\n", maintVer);

        // 0x13: Codepage (RS, 2 bytes)
        short codepage = buf.getShort();
        System.out.printf("Codepage: %d (0x%04X)\n", codepage, codepage & 0xFFFF);

        // 0x15: Number of sections (RC, 1 byte)
        int sectionCount = buf.get() & 0xFF;
        System.out.printf("Number of sections: %d\n", sectionCount);

        System.out.println("\n=== Section Locators ===");
        // Read section locators (12 bytes each, same as R13)
        // Format: RL (4 bytes) RL (4 bytes) RL (4 bytes)
        //         record_number, offset, size

        long objectsOffset = -1;
        long objectsSize = -1;
        long headerOffset = -1;
        long headerSize = -1;

        for (int i = 0; i < sectionCount; i++) {
            int recordNum = buf.getInt();
            int offset = buf.getInt();
            int size = buf.getInt();

            String sectionName = getSectionName(recordNum);
            System.out.printf("Locator[%d]: record=%d, offset=0x%X (%d bytes), size=0x%X (%d bytes) - %s\n",
                i, recordNum, offset, offset, size, size, sectionName);

            if (recordNum == 0) {
                headerOffset = offset;
                headerSize = size;
            } else if (recordNum == 3) {
                objectsOffset = offset;
                objectsSize = size;
            }
        }

        // R2000 special handling
        System.out.println("\n=== R2000 Objects Section Calculation ===");
        long currentPos = buf.position();
        System.out.printf("Current position in header: 0x%X (%d bytes)\n", currentPos, currentPos);

        // Objects section starts after locators
        long locatorsEnd = currentPos;
        System.out.printf("Objects section starts at: 0x%X (%d bytes)\n", locatorsEnd, locatorsEnd);

        // Objects section ends at Header section start
        if (headerOffset > 0) {
            System.out.printf("Header section starts at: 0x%X (%d bytes)\n", headerOffset, headerOffset);

            long calculatedObjectsSize = headerOffset - locatorsEnd;
            System.out.printf("Calculated Objects size: 0x%X (%d bytes)\n", calculatedObjectsSize, calculatedObjectsSize);

            // Show first bytes of Objects section
            System.out.println("\n=== First bytes of Objects section ===");
            int sampleSize = Math.min(256, (int) calculatedObjectsSize);
            System.out.printf("Showing first %d bytes from 0x%X:\n", sampleSize, locatorsEnd);

            byte[] sample = new byte[sampleSize];
            if (locatorsEnd + sampleSize <= data.length) {
                System.arraycopy(data, (int) locatorsEnd, sample, 0, sampleSize);

                // Hex dump
                for (int i = 0; i < sampleSize; i += 16) {
                    System.out.printf("0x%04X: ", locatorsEnd + i);
                    for (int j = 0; j < 16 && i + j < sampleSize; j++) {
                        System.out.printf("%02X ", sample[i + j] & 0xFF);
                    }
                    System.out.print(" | ");
                    for (int j = 0; j < 16 && i + j < sampleSize; j++) {
                        byte b = sample[i + j];
                        if (b >= 32 && b <= 126) {
                            System.out.print((char) b);
                        } else {
                            System.out.print(".");
                        }
                    }
                    System.out.println();
                }
            }
        }

        System.out.println("\n=== CRC Checksum ===");
        short crc = buf.getShort();
        System.out.printf("CRC: 0x%04X\n", crc & 0xFFFF);
    }

    private static String getSectionName(int recordNum) {
        return switch (recordNum) {
            case 0 -> "Header";
            case 1 -> "Classes";
            case 2 -> "Handles";
            case 3 -> "Objects";
            default -> "Unknown/Auxiliary";
        };
    }
}
