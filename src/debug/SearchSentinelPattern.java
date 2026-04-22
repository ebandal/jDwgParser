package debug;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SearchSentinelPattern {
    public static void main(String[] args) throws Exception {
        String[] files = {"samples/2000/Arc.dwg", "samples/2000/circle.dwg", "samples/2000/Cone.dwg"};
        
        for (String filePath : files) {
            System.out.printf("\n=== %s ===\n", filePath);
            byte[] data = Files.readAllBytes(Paths.get(filePath));
            
            // Objects section: 0x60 to 0x6B8A (27,435 bytes)
            int objectsStart = 0x60;
            int objectsSize = 0x6B8B - 0x60;  // = 27,435
            
            byte[] objectsSection = new byte[objectsSize];
            System.arraycopy(data, objectsStart, objectsSection, 0, objectsSize);
            
            System.out.printf("Objects section: 0x60-0x6B8A (%d bytes)\n", objectsSize);
            
            // R2000 Sentinel pattern (from spec: 0xCF 0x43 0xCB 0x51)
            byte[] sentinelStart = {(byte)0xCF, (byte)0x43, (byte)0xCB, (byte)0x51};
            byte[] sentinelEnd = {(byte)0x30, (byte)0x84, (byte)0x34, (byte)0xAE};
            
            System.out.println("Searching for Sentinels...");
            
            // Search for start sentinel
            for (int i = 0; i < objectsSize - 16; i++) {
                boolean match = true;
                for (int j = 0; j < 4; j++) {
                    if (objectsSection[i + j] != sentinelStart[j]) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    System.out.printf("  Start Sentinel found at offset 0x%04X\n", i);
                    System.out.printf("    Bytes: %02X %02X %02X %02X\n",
                        objectsSection[i] & 0xFF, objectsSection[i+1] & 0xFF,
                        objectsSection[i+2] & 0xFF, objectsSection[i+3] & 0xFF);
                }
            }
            
            // Also search for any consecutive RS_BE values (page_size markers)
            System.out.println("Searching for consecutive RS_BE values (Handles pages pattern)...");
            ByteBuffer buf = ByteBuffer.wrap(objectsSection);
            buf.order(ByteOrder.BIG_ENDIAN);
            
            int consecutiveCount = 0;
            int lastOffset = -100;
            
            for (int i = 0; i < objectsSize - 1; i++) {
                buf.position(i);
                if (buf.remaining() >= 2) {
                    short valueBE = buf.getShort();
                    int value = valueBE & 0xFFFF;
                    
                    if (value >= 2000 && value <= 2100) {
                        if (i - lastOffset < 50) {
                            // Another page size marker nearby
                            consecutiveCount++;
                            System.out.printf("  Multiple page sizes near offset 0x%04X: 0x%04X, 0x%04X\n",
                                lastOffset, lastOffset, i);
                        }
                        lastOffset = i;
                    }
                }
            }
        }
    }
}
