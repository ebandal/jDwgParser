import java.nio.file.*;
public class check_section_map {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("samples/2004/Arc.dwg"));
        
        // From debug output: Section map at 0xF340
        // But actual file offset = (0xF340 + 0x100) bytes
        int fileOffset = 0xF440;
        
        System.out.println("File bytes at 0x" + Integer.toHexString(fileOffset) + ":");
        for (int i = 0; i < Math.min(160, data.length - fileOffset); i += 16) {
            System.out.printf("  0x%02X: ", i);
            for (int j = 0; j < 16 && i + j < Math.min(160, data.length - fileOffset); j++) {
                System.out.printf("%02X ", data[fileOffset + i + j] & 0xFF);
            }
            System.out.println();
        }
        
        // Skip the 0x100 page header and look at actual compressed data
        System.out.println("\nCompressed section map data (after 0x100 header):");
        int dataStart = fileOffset + 0x100;
        for (int i = 0; i < Math.min(64, data.length - dataStart); i += 16) {
            System.out.printf("  0x%02X: ", i);
            for (int j = 0; j < 16 && i + j < Math.min(64, data.length - dataStart); j++) {
                System.out.printf("%02X ", data[dataStart + i + j] & 0xFF);
            }
            System.out.println();
        }
    }
}
