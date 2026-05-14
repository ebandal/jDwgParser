package debug;

import java.nio.file.Files;
import java.nio.file.Paths;

public class VerifyObjectsExtraction {
    public static void main(String[] args) throws Exception {
        byte[] fileData = Files.readAllBytes(Paths.get("samples/2000/Arc.dwg"));
        
        System.out.println("=== Verify Objects Section Extraction ===\n");
        
        // File structure according to R2000FileStructureHandler:
        // Objects: offset=0x60, size=27435 bytes
        
        int objectsFileOffset = 0x60;
        int objectsSize = 27435;
        
        System.out.printf("Objects section in file:\n");
        System.out.printf("  File offset: 0x%X\n", objectsFileOffset);
        System.out.printf("  Size: %d bytes\n", objectsSize);
        System.out.printf("  End: 0x%X\n", objectsFileOffset + objectsSize);
        
        System.out.printf("\nFirst 64 bytes (as extracted):\n");
        for (int i = 0; i < 64; i++) {
            if (i % 16 == 0) System.out.printf("0x%02X: ", i);
            System.out.printf("%02X ", fileData[objectsFileOffset + i] & 0xFF);
            if ((i + 1) % 16 == 0) System.out.println();
        }
        
        // Check what's there
        System.out.println();
        if (fileData[objectsFileOffset] == 0x00 && fileData[objectsFileOffset+1] == (byte)0xFF) {
            System.out.println("✓ Objects section starts with 00 FF (object marker)");
        } else if (fileData[objectsFileOffset] == 0x41 && fileData[objectsFileOffset+1] == 0x43) {
            System.out.println("✗ Objects section starts with AC (version string) - WRONG!");
        }
        
        // Also check what's at 0x60 from another reference
        System.out.printf("\nFile bytes at 0x60-0x80:\n");
        for (int i = 0x60; i < 0x80; i++) {
            if ((i - 0x60) % 16 == 0) System.out.printf("File 0x%02X: ", i);
            System.out.printf("%02X ", fileData[i] & 0xFF);
            if ((i - 0x60 + 1) % 16 == 0) System.out.println();
        }
    }
}
