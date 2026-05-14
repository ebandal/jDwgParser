package debug;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DebugR2000Offset {
    public static void main(String[] args) throws Exception {
        byte[] fileData = Files.readAllBytes(Paths.get("samples/2000/Arc.dwg"));
        
        System.out.println("=== Arc.dwg Analysis ===\n");
        
        // Extract Objects section starting at 0x60
        int objectsStart = 0x60;
        byte[] objectsSection = new byte[fileData.length - objectsStart];
        System.arraycopy(fileData, objectsStart, objectsSection, 0, objectsSection.length);
        
        System.out.printf("Objects section size: %d bytes\n", objectsSection.length);
        System.out.printf("File data length: %d bytes\n", fileData.length);
        
        // Show first 20 bytes
        System.out.println("\nFirst 20 bytes of Objects section:");
        for (int i = 0; i < Math.min(20, objectsSection.length); i++) {
            System.out.printf("Offset 0x%02X: 0x%02X\n", i, objectsSection[i] & 0xFF);
        }
        
        // Find all big-endian shorts in range 2000-2100
        ByteBuffer buf = ByteBuffer.wrap(objectsSection);
        buf.order(ByteOrder.BIG_ENDIAN);
        
        System.out.println("\nScanning for RS_BE values (2000-2100) in Objects section:");
        for (int i = 0; i < objectsSection.length - 1; i++) {
            buf.position(i);
            if (buf.remaining() >= 2) {
                short valueBE = buf.getShort();
                int value = valueBE & 0xFFFF;
                if (value >= 2000 && value <= 2100) {
                    System.out.printf("  Offset 0x%02X: value=%d (bytes: 0x%02X 0x%02X)\n", 
                        i, value, 
                        objectsSection[i] & 0xFF, 
                        objectsSection[i+1] & 0xFF);
                }
            }
        }
    }
}
