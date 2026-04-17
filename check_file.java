import java.nio.file.*;
public class check_file {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("samples/2004/Arc.dwg"));
        System.out.println("File size: " + data.length);
        
        // Check structure near beginning and end
        System.out.println("\nFirst 256 bytes:");
        for (int i = 0; i < Math.min(256, data.length); i += 16) {
            System.out.printf("  0x%03X: ", i);
            for (int j = 0; j < 16 && i + j < data.length; j++) {
                System.out.printf("%02X ", data[i + j] & 0xFF);
            }
            System.out.println();
        }
        
        // Check for patterns at likely offsets
        System.out.println("\nBytes around 0x100:");
        for (int i = 0x0F0; i < 0x120 && i < data.length; i += 16) {
            System.out.printf("  0x%03X: ", i);
            for (int j = 0; j < 16 && i + j < data.length; j++) {
                System.out.printf("%02X ", data[i + j] & 0xFF);
            }
            System.out.println();
        }
    }
}
