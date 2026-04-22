package debug;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Analyze R2000 object offset patterns to find correct spacing
 */
public class AnalyzeR2000ObjectOffsets {
    public static void main(String[] args) throws Exception {
        Path testFile = Paths.get("C:\\workspace_ebandal\\jDwgParser\\samples\\2000\\Arc.dwg");
        byte[] fileData = Files.readAllBytes(testFile);
        byte[] objectsData = new byte[0x6B8B - 0x60];
        System.arraycopy(fileData, 0x60, objectsData, 0, objectsData.length);

        System.out.println("=== R2000 Object Offset Analysis (Arc.dwg) ===\n");

        int objectIndex = 0;
        int offset = 0;
        int maxToAnalyze = 20;

        while (offset < objectsData.length - 10 && objectIndex < maxToAnalyze) {
            // Look for 00 FF marker
            if ((objectsData[offset] & 0xFF) == 0x00 &&
                (objectsData[offset + 1] & 0xFF) == 0xFF) {

                // Read size (RS, little-endian)
                short size = ByteBuffer.wrap(objectsData, offset + 2, 2)
                    .order(ByteOrder.LITTLE_ENDIAN).getShort();

                int type = objectsData[offset + 8] & 0xFF;
                boolean validType = (type >= 0x30 && type <= 0x4F);

                System.out.printf("Object %d @ 0x%04X:\n", objectIndex, offset);
                System.out.printf("  Size field: %d (0x%04X)\n", size & 0xFFFF, size & 0xFFFF);
                System.out.printf("  Type: 0x%02X %s\n", type, validType ? "" : "(INVALID)");

                if (validType) {
                    // Try to find next valid marker
                    int nextValid = -1;
                    for (int search = offset + 2; search < Math.min(offset + 1000, objectsData.length - 1); search++) {
                        if ((objectsData[search] & 0xFF) == 0x00 &&
                            (objectsData[search + 1] & 0xFF) == 0xFF &&
                            search + 10 < objectsData.length) {
                            int nextType = objectsData[search + 8] & 0xFF;
                            if ((nextType >= 0x30 && nextType <= 0x4F)) {
                                nextValid = search;
                                break;
                            }
                        }
                    }

                    if (nextValid > 0) {
                        int distance = nextValid - offset;
                        int dataSize = size & 0xFFFF;
                        int overhead = distance - dataSize;
                        System.out.printf("  Next valid @ 0x%04X: distance=%d, overhead=%d bytes\n",
                            nextValid, distance, overhead);
                    } else {
                        System.out.printf("  Next valid: NOT FOUND\n");
                    }

                    // Move past this object if type is valid
                    if (nextValid > offset) {
                        offset = nextValid;
                    } else {
                        offset += 2;
                    }
                    objectIndex++;
                } else {
                    offset += 2;
                }
            } else {
                offset += 1;
            }

            System.out.println();
        }

        System.out.printf("Valid objects found: %d\n", objectIndex);
    }
}
