package debug;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AnalyzeR2000OffsetBase {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("samples/2000/Arc.dwg"));
        
        // Objects section: 0x60 to 0x6B8B
        int objectsStart = 0x60;
        int handlesStart = 0x60 + 0x7F;  // Within Objects section
        
        System.out.println("=== R2000 Arc.dwg Offset Analysis ===\n");
        
        System.out.printf("File structure:\n");
        System.out.printf("  File start: 0x00\n");
        System.out.printf("  Objects section: 0x%02X (file offset)\n", objectsStart);
        System.out.printf("  Handles section: 0x%02X (file offset) = 0x%02X (Objects offset)\n", 
            handlesStart, 0x7F);
        System.out.printf("  Handles end: ~0x%02X (0x7F + 0x7EC ≈ 0x88B)\n", handlesStart + 0x7EC);
        
        // The Handles offsets from parsing
        int[] handleOffsets = {40, -2973, -541, 43, 0};
        
        System.out.println("\n=== Testing different offset bases ===\n");
        
        // Base 1: offset 0 in Objects section
        System.out.println("Base 1: Objects section start (0x60 in file)");
        int cumulativeOffset = 0;
        for (int i = 0; i < handleOffsets.length; i++) {
            cumulativeOffset += handleOffsets[i];
            int fileOffset = objectsStart + cumulativeOffset;
            System.out.printf("  Handle %d: cumulative=%d, file_offset=0x%02X ", 
                i, cumulativeOffset, fileOffset);
            if (fileOffset < 0 || fileOffset > data.length) {
                System.out.println("❌ OUT OF RANGE");
            } else {
                System.out.printf("✓ [%02X %02X %02X %02X]\n",
                    data[fileOffset] & 0xFF,
                    (fileOffset+1 < data.length) ? (data[fileOffset+1] & 0xFF) : 0,
                    (fileOffset+2 < data.length) ? (data[fileOffset+2] & 0xFF) : 0,
                    (fileOffset+3 < data.length) ? (data[fileOffset+3] & 0xFF) : 0);
            }
        }
        
        // Base 2: offset from Handles section start
        System.out.println("\nBase 2: Handles section start (0x7F within Objects)");
        cumulativeOffset = 0;
        for (int i = 0; i < handleOffsets.length; i++) {
            cumulativeOffset += handleOffsets[i];
            int fileOffset = handlesStart + cumulativeOffset;
            System.out.printf("  Handle %d: cumulative=%d, file_offset=0x%02X ", 
                i, cumulativeOffset, fileOffset);
            if (fileOffset < 0 || fileOffset > data.length) {
                System.out.println("❌ OUT OF RANGE");
            } else {
                System.out.printf("✓ [%02X %02X %02X %02X]\n",
                    data[fileOffset] & 0xFF,
                    (fileOffset+1 < data.length) ? (data[fileOffset+1] & 0xFF) : 0,
                    (fileOffset+2 < data.length) ? (data[fileOffset+2] & 0xFF) : 0,
                    (fileOffset+3 < data.length) ? (data[fileOffset+3] & 0xFF) : 0);
            }
        }
        
        // Base 3: Check what's at offset 40 from Objects start
        System.out.println("\nBase 3: What's at offset 40 in Objects section?");
        int pos = objectsStart + 40;
        System.out.printf("At Objects[0x%02X] (file 0x%02X): ", 40, pos);
        for (int i = 0; i < 32 && pos + i < data.length; i++) {
            byte b = data[pos + i];
            if (b >= 32 && b < 127) {
                System.out.print((char)b);
            } else {
                System.out.printf("[%02X]", b & 0xFF);
            }
        }
        System.out.println();
        
        // Base 4: Check Handles location more carefully
        System.out.println("\nBase 4: Verify Handles location at 0x7F");
        System.out.printf("At Objects[0x7F] (file 0x%02X):\n", handlesStart);
        for (int i = 0; i < 64 && handlesStart + i < data.length; i++) {
            if (i % 16 == 0) System.out.printf("  0x%02X: ", i);
            System.out.printf("%02X ", data[handlesStart + i] & 0xFF);
            if ((i + 1) % 16 == 0) System.out.println();
        }
    }
}
