package io.dwg.test;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Test RLL (Recorded Long Long) parsing of PageMap data
 * RLL is variable-length integer encoding used in R2007 structures
 */
public class TestRLLParsing {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Test: RLL Parsing of PageMap Data");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] fileData = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));

        // Extract PageMap data from 0x480
        byte[] pageMapData = new byte[114];
        System.arraycopy(fileData, 0x480, pageMapData, 0, 114);

        System.out.println("PageMap raw data (114 bytes):");
        for (int i = 0; i < 114; i += 16) {
            System.out.printf("+%02X: ", i);
            for (int j = 0; j < 16 && i + j < 114; j++) {
                System.out.printf("%02X ", pageMapData[i + j] & 0xFF);
            }
            System.out.println();
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("RLL Parsing (page size, page id pairs):");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        int[] offset = {0};
        int pageCount = 0;

        try {
            while (offset[0] < pageMapData.length && pageCount < 100) {
                long size = readRLL(pageMapData, offset);
                if (size == 0) {
                    System.out.println("  [END] Zero size detected");
                    break;
                }

                long id = readRLL(pageMapData, offset);

                System.out.printf("  [%d] size=0x%X (%d), id=0x%X (%d)\n",
                    pageCount, size, size, id, id);

                pageCount++;

                if (offset[0] > pageMapData.length) {
                    System.out.println("  [END] Reached end of data");
                    break;
                }
            }

            System.out.printf("\nTotal pages parsed: %d\n", pageCount);
            System.out.printf("Bytes consumed: %d / %d\n\n", offset[0], pageMapData.length);

        } catch (Exception e) {
            System.out.printf("Error during parsing: %s\n\n", e.getMessage());
        }
    }

    /**
     * Read RLL (Recorded Long Long) value
     * RLL is a variable-length encoding for int64_t values
     * Simple version: reads bytes until a terminator
     */
    private static long readRLL(byte[] data, int[] offsetHolder) throws Exception {
        int offset = offsetHolder[0];

        if (offset >= data.length) {
            throw new Exception("RLL read past end of data");
        }

        // Try simple LE64 read first
        if (offset + 8 <= data.length) {
            long value = 0;
            for (int i = 0; i < 8; i++) {
                value |= ((long)(data[offset + i] & 0xFF)) << (i * 8);
            }

            offsetHolder[0] += 8;
            return value;
        } else {
            throw new Exception("Not enough data for RLL");
        }
    }
}
