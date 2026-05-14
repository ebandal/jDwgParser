package debug;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CheckObjectsSectionStart {
    public static void main(String[] args) throws Exception {
        String[] files = {"samples/2000/Arc.dwg", "samples/2000/circle.dwg", "samples/2000/Cone.dwg"};
        
        // R13/R14 Sentinel patterns
        byte[] classStartSentinel = {(byte)0xCF, (byte)0x43, (byte)0xCB, (byte)0x51, 
                                    (byte)0xAE, (byte)0x60, (byte)0xC9, (byte)0x41,
                                    (byte)0xC0, (byte)0x5A, (byte)0xF0, (byte)0x4F,
                                    (byte)0x28, (byte)0xB0, (byte)0x53, (byte)0x0F};
        
        byte[] handlesStartSentinel = {(byte)0x96, (byte)0xA0, (byte)0x82, (byte)0x1A,
                                       (byte)0x60, (byte)0xE8, (byte)0x10, (byte)0x5A,
                                       (byte)0x98, (byte)0x40, (byte)0xE8, (byte)0x21,
                                       (byte)0x42, (byte)0x0E, (byte)0xAC, (byte)0x05};
        
        for (String filePath : files) {
            System.out.printf("\n=== %s ===\n", filePath);
            byte[] data = Files.readAllBytes(Paths.get(filePath));
            
            // Objects section starts at 0x60, size is header_offset - 0x60
            int objectsStart = 0x60;
            
            System.out.printf("Objects section starts at offset 0x%02X\n", objectsStart);
            System.out.println("First 128 bytes (hex):");
            
            for (int i = 0; i < Math.min(128, data.length - objectsStart); i++) {
                if (i % 16 == 0) System.out.printf("0x%02X: ", objectsStart + i);
                System.out.printf("%02X ", data[objectsStart + i] & 0xFF);
                if ((i + 1) % 16 == 0) System.out.println();
            }
            System.out.println("\n");
            
            // Check for Sentinels
            System.out.println("Checking for Sentinels:");
            
            // Check first bytes against known Sentinels
            boolean hasClassSentinel = false;
            boolean hasHandlesSentinel = false;
            
            for (int i = 0; i <= 32 && i < data.length - objectsStart - 16; i++) {
                boolean matchClass = true;
                boolean matchHandles = true;
                
                for (int j = 0; j < 16; j++) {
                    if (data[objectsStart + i + j] != classStartSentinel[j]) matchClass = false;
                    if (data[objectsStart + i + j] != handlesStartSentinel[j]) matchHandles = false;
                }
                
                if (matchClass) {
                    System.out.printf("  ✓ Classes sentinel found at offset 0x%02X\n", objectsStart + i);
                    hasClassSentinel = true;
                }
                if (matchHandles) {
                    System.out.printf("  ✓ Handles sentinel found at offset 0x%02X\n", objectsStart + i);
                    hasHandlesSentinel = true;
                }
            }
            
            if (!hasClassSentinel && !hasHandlesSentinel) {
                System.out.println("  ✗ No known Sentinels found in first 32 bytes");
            }
            
            // Check what's at 0x60: the "AC1015" string
            System.out.printf("\nData at 0x%02X: ", objectsStart);
            for (int i = 0; i < 16 && objectsStart + i < data.length; i++) {
                byte b = data[objectsStart + i];
                if (b >= 32 && b < 127) {
                    System.out.print((char)b);
                } else {
                    System.out.printf("[%02X]", b & 0xFF);
                }
            }
            System.out.println();
        }
    }
}
