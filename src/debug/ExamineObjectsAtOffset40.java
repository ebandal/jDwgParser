package debug;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ExamineObjectsAtOffset40 {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("samples/2000/Arc.dwg"));
        
        int objectsStart = 0x60;  // Objects section in file
        int offset40 = objectsStart + 40;  // Where first handle points
        
        System.out.println("=== What's at Objects[40] ===\n");
        
        System.out.printf("File offset: 0x%X\n", offset40);
        System.out.printf("First 64 bytes (hex):\n");
        
        for (int i = 0; i < 64; i++) {
            if (i % 16 == 0) System.out.printf("0x%02X: ", i);
            System.out.printf("%02X ", data[offset40 + i] & 0xFF);
            if ((i + 1) % 16 == 0) System.out.println();
        }
        
        // Try to parse as object
        System.out.println("\n=== Attempt to parse as Object ===\n");
        
        ByteBuffer buf = ByteBuffer.wrap(data, offset40, data.length - offset40);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        
        // Read MS (Modular Short) - object size
        int size = readModularShort(buf);
        System.out.printf("Object size (MS): %d bytes\n", size);
        
        // Read BS (Bit Short) - type code
        int typeCode = readBitShort(buf);
        System.out.printf("Type code (BS): %d (0x%X)\n", typeCode, typeCode);
        
        System.out.println("\n=== Alternatively, try reading from offset 0 (Objects start) ===\n");
        
        buf = ByteBuffer.wrap(data, objectsStart, data.length - objectsStart);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        
        System.out.printf("First 128 bytes at Objects[0]:\n");
        for (int i = 0; i < 128; i++) {
            if (i % 16 == 0) System.out.printf("0x%02X: ", i);
            byte b = buf.get(i);
            System.out.printf("%02X ", b & 0xFF);
            if ((i + 1) % 16 == 0) System.out.println();
        }
        
        // Look for object markers
        System.out.println("\n=== Searching for object markers (00 FF pattern) ===\n");
        
        for (int i = 0; i < 200; i++) {
            if (data[objectsStart + i] == 0x00 && data[objectsStart + i + 1] == (byte)0xFF) {
                System.out.printf("Found 00 FF at Objects[0x%02X]\n", i);
            }
        }
    }
    
    static int readModularShort(ByteBuffer buf) {
        int value = 0;
        int shift = 0;
        while (true) {
            byte b = buf.get();
            value |= ((b & 0x7F) << shift);
            if ((b & 0x80) == 0) break;
            shift += 7;
        }
        return value;
    }
    
    static int readBitShort(ByteBuffer buf) {
        // Just read as little-endian short for now
        return buf.getShort() & 0xFFFF;
    }
}
