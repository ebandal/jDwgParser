import java.nio.file.*;
public class check_layout {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("../samples/2004/Arc.dwg"));
        System.out.println("File size: " + data.length);
        
        // 파일 헤더에서 section map offset 찾기
        // 암호화된 헤더 (0x7A-0xE5) 복호화해서 section map offset 찾기
        
        // First read plain header fields at 0x7F-0xE5
        System.out.println("\nPlain header bytes 0x7F-0xDF:");
        for (int i = 0x7F; i < 0xE0; i += 16) {
            System.out.printf("  0x%02X: ", i);
            for (int j = 0; j < 16 && i + j < 0xE0; j++) {
                System.out.printf("%02X ", data[i + j] & 0xFF);
            }
            System.out.println();
        }
        
        // Section map address is at 0x54-0x5B (from decrypted header)
        // Let's also check if there's structure before 0x100
        System.out.println("\nBytes at 0xF0-0x110:");
        for (int i = 0xF0; i < 0x110; i += 16) {
            System.out.printf("  0x%02X: ", i);
            for (int j = 0; j < 16 && i + j < Math.min(0x110, data.length); j++) {
                System.out.printf("%02X ", data[i + j] & 0xFF);
            }
            System.out.println();
        }
    }
}
