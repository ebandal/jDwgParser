package debug;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AnalyzeR2000Locators {
    public static void main(String[] args) throws Exception {
        String[] files = {"samples/2000/Arc.dwg", "samples/2000/circle.dwg", "samples/2000/Cone.dwg"};
        
        for (String filePath : files) {
            System.out.printf("\n=== %s ===\n", filePath);
            byte[] data = Files.readAllBytes(Paths.get(filePath));
            
            System.out.printf("File size: %d bytes\n", data.length);
            
            // After DWG header (0x19 = 25 bytes), read section locators
            // Each locator: 4B number + 4B address + 4B size = 12 bytes
            
            System.out.println("\nSection Locators (starting at offset 0x19):");
            for (int i = 0; i < 6; i++) {
                int offset = 0x19 + (i * 12);
                if (offset + 12 > data.length) break;
                
                ByteBuffer buf = ByteBuffer.wrap(data, offset, 12);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                
                int number = buf.getInt();
                int address = buf.getInt();
                int size = buf.getInt();
                
                String sectionName = "";
                if (number == 0) sectionName = "Header";
                else if (number == 1) sectionName = "Classes";
                else if (number == 2) sectionName = "Handles";
                else if (number == 3) sectionName = "ObjFreeSpace";
                else if (number == 4) sectionName = "Template";
                else if (number == 5) sectionName = "AuxHeader";
                
                System.out.printf("  Locator[%d]: number=%d %s, address=0x%X (%d), size=%d\n",
                    i, number, sectionName, address, address, size);
                    
                // Check if address is in valid range
                if (address > 0 && address < data.length && size > 0 && address + size <= data.length) {
                    System.out.printf("    ✓ Valid range [0x%X, 0x%X)\n", address, address + size);
                    
                    // Show first few bytes at this location
                    System.out.printf("    First 16 bytes: ");
                    for (int j = 0; j < Math.min(16, size); j++) {
                        System.out.printf("%02X ", data[address + j] & 0xFF);
                    }
                    System.out.println();
                } else {
                    System.out.printf("    ✗ INVALID (out of bounds or zero-size)\n");
                }
            }
        }
    }
}
