package debug;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ExamineR2000Structure {
    public static void main(String[] args) throws Exception {
        String[] files = {"samples/2000/Arc.dwg", "samples/2000/circle.dwg", "samples/2000/Cone.dwg"};
        
        for (String filePath : files) {
            System.out.printf("\n=== %s ===\n", filePath);
            byte[] data = Files.readAllBytes(Paths.get(filePath));
            
            // Objects section starts at offset 0x60
            ByteBuffer buf = ByteBuffer.wrap(data, 0x60, Math.min(300, data.length - 0x60));
            buf.order(ByteOrder.BIG_ENDIAN);
            
            System.out.println("First 200 bytes of Objects section (offset 0x60):");
            for (int i = 0; i < Math.min(200, buf.capacity()); i += 16) {
                System.out.printf("0x%02X: ", 0x60 + i);
                for (int j = 0; j < 16 && i + j < buf.capacity(); j++) {
                    System.out.printf("%02X ", buf.get(i + j) & 0xFF);
                }
                System.out.println();
            }
            
            // Check what big-endian shorts we have at key offsets
            buf.rewind();
            System.out.println("\nBig-endian shorts (potential page sizes) at early offsets:");
            for (int i = 0; i < Math.min(100, buf.capacity() - 1); i += 2) {
                byte[] twoBytes = new byte[2];
                buf.get(i, twoBytes);
                int val = ((twoBytes[0] & 0xFF) << 8) | (twoBytes[1] & 0xFF);
                if (val >= 2000 && val <= 2100) {
                    System.out.printf("  Offset 0x%02X: 0x%04X (%d)\n", 0x60 + i, val, val);
                }
            }
        }
    }
}
