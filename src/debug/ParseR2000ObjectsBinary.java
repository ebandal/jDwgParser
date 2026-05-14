package debug;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Parse R2000 Objects section as binary format (not bit-packed)
 */
public class ParseR2000ObjectsBinary {
    public static void main(String[] args) throws Exception {
        Path testFile = Paths.get("C:\\workspace_ebandal\\jDwgParser\\samples\\2000\\Arc.dwg");
        byte[] fileData = Files.readAllBytes(testFile);

        // Extract Objects section (0x60 - 0x6B8B)
        byte[] objectsData = new byte[0x6B8B - 0x60];
        System.arraycopy(fileData, 0x60, objectsData, 0, objectsData.length);

        System.out.println("=== R2000 Objects Section Binary Format ===");
        System.out.printf("Section size: %d bytes\n\n", objectsData.length);

        int offset = 0;
        int objectCount = 0;
        int maxObjects = 10;

        while (offset < objectsData.length && objectCount < maxObjects) {
            if (offset + 8 > objectsData.length) break;

            System.out.printf("Object %d @ 0x%04X:\n", objectCount, offset);

            // Try the discovered pattern:
            // 00 FF [size:RL] 1F 00 [count:RC] 00 [type:RC] [data]

            byte b0 = objectsData[offset];
            byte b1 = objectsData[offset + 1];

            System.out.printf("  [+0x00] 0x%02X 0x%02X", b0 & 0xFF, b1 & 0xFF);

            if ((b0 & 0xFF) == 0x00 && (b1 & 0xFF) == 0xFF) {
                System.out.println(" <- MARKER FOUND");

                // Read size (RS = 2 bytes, little-endian)
                short size = ByteBuffer.wrap(objectsData, offset + 2, 2)
                    .order(ByteOrder.LITTLE_ENDIAN).getShort();
                System.out.printf("  [+0x02] Size (RS): 0x%04X (%d bytes)\n", size & 0xFFFF, size & 0xFFFF);

                // Next should be 1F 00
                byte b4 = objectsData[offset + 4];
                byte b5 = objectsData[offset + 5];
                System.out.printf("  [+0x04] 0x%02X 0x%02X\n", b4 & 0xFF, b5 & 0xFF);

                // Count
                byte count = objectsData[offset + 6];
                System.out.printf("  [+0x06] Count: 0x%02X (%d)\n", count & 0xFF, count & 0xFF);

                // Next byte should be 00
                byte b7 = objectsData[offset + 7];
                System.out.printf("  [+0x07] 0x%02X\n", b7 & 0xFF);

                // Type
                byte type = objectsData[offset + 8];
                System.out.printf("  [+0x08] Type: 0x%02X\n", type & 0xFF);
                printTypeName(type & 0xFF);

                // Next byte should be 00
                if (offset + 9 < objectsData.length) {
                    byte b9 = objectsData[offset + 9];
                    System.out.printf("  [+0x09] 0x%02X\n", b9 & 0xFF);
                }

                // Object data starts at offset+10
                System.out.printf("  Object data starts at 0x%04X, size %d bytes\n",
                    offset + 10, Math.max(0, (size & 0xFFFF) - 8));

                // Move to next object
                offset += (size & 0xFFFF) + 2;  // size + marker (00 FF RS)
                System.out.println();

                objectCount++;
            } else {
                System.out.println(" <- NO MARKER, moving ahead");
                offset += 2;
            }
        }

        System.out.printf("Total objects parsed: %d\n", objectCount);
    }

    static void printTypeName(int typeCode) {
        String typeName = switch (typeCode) {
            case 0x30 -> "BLOCK_HEADER";
            case 0x32 -> "LTYPE";
            case 0x33 -> "LAYER";
            case 0x34 -> "STYLE";
            case 0x35 -> "?";
            case 0x38 -> "?";
            case 0x3B -> "?";
            case 0x3C -> "?";
            case 0x3D -> "?";
            case 0x3E -> "?";
            default -> "UNKNOWN";
        };
        System.out.printf("           = %s\n", typeName);
    }
}
