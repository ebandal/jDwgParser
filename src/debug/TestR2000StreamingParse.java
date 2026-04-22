package debug;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestR2000StreamingParse {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("samples/2000/Arc.dwg"));
        int objectsStart = 0x60;
        
        System.out.println("=== R2000 Streaming Parse Test ===\n");
        
        byte[] objectsSection = new byte[27435];
        System.arraycopy(data, objectsStart, objectsSection, 0, 27435);
        
        // Try to stream parse from offset 0x60 (relative offset within section)
        System.out.println("Attempt 1: Parse from Objects[0x60] (where objects seem to start)");
        streamParseFrom(objectsSection, 0x60);
        
        // Try from offset 0
        System.out.println("\nAttempt 2: Parse from Objects[0x00]");
        streamParseFrom(objectsSection, 0x00);
    }
    
    static void streamParseFrom(byte[] section, int startOffset) {
        System.out.printf("Starting at offset 0x%02X\n", startOffset);
        
        int offset = startOffset;
        int count = 0;
        
        while (offset < section.length - 6 && count < 10) {
            byte b0 = section[offset];
            byte b1 = (offset + 1 < section.length) ? section[offset + 1] : 0;
            
            System.out.printf("  Offset 0x%04X: %02X %02X ", offset, b0 & 0xFF, b1 & 0xFF);
            
            // Look for object markers or structure
            if (b0 == 0x00 && (b1 & 0xFF) >= 0xFE) {
                System.out.printf("← potential object marker (00 FE/FF)");
                
                // Try to read object size
                if (offset + 3 < section.length) {
                    ByteBuffer buf = ByteBuffer.wrap(section, offset, section.length - offset);
                    buf.order(ByteOrder.LITTLE_ENDIAN);
                    
                    int b0_val = buf.get() & 0xFF;
                    int b1_val = buf.get() & 0xFF;
                    int size = readMS(buf);
                    
                    System.out.printf(" size=%d", size);
                    offset += size;
                }
            }
            
            System.out.println();
            offset += 2;
            count++;
        }
    }
    
    static int readMS(ByteBuffer buf) {
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
}
