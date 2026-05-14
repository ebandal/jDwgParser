package debug;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.format.r2000.R2000FileStructureHandler;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.core.io.SectionInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class ParseR2000Header {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("samples/2000/Leader.dwg"));
        
        BitInput input = new ByteBufferBitInput(data);
        R2000FileStructureHandler handler = new R2000FileStructureHandler();
        FileHeaderFields header = handler.readHeader(input);
        
        input = new ByteBufferBitInput(data);
        Map<String, SectionInputStream> sections = handler.readSections(input, header);
        SectionInputStream objectsSection = sections.get("AcDb:AcDbObjects");
        
        byte[] objData = objectsSection.rawBytes();
        System.out.println("=== R2000 Objects Section Header Analysis ===\n");
        System.out.printf("Total size: 0x%X bytes\n\n", objData.length);
        
        // Try to interpret as structure
        System.out.println("【 Interpretation 1: Fixed Layout 】");
        System.out.printf("0x00: Fixed markers: %02X %02X\n", objData[0] & 0xFF, objData[1] & 0xFF);
        
        int size1 = (objData[2] & 0xFF) | ((objData[3] & 0xFF) << 8);
        System.out.printf("0x02-0x03: LE16 = 0x%04X (%d)\n", size1, size1);
        
        System.out.printf("0x04: %02X\n", objData[4] & 0xFF);
        System.out.printf("0x05: %02X\n", objData[5] & 0xFF);
        
        int size2 = (objData[6] & 0xFF) | ((objData[7] & 0xFF) << 8);
        System.out.printf("0x06-0x07: LE16 = 0x%04X (%d)\n", size2, size2);
        
        System.out.printf("0x08: %02X (type?)\n", objData[8] & 0xFF);
        System.out.printf("0x09: %02X\n", objData[9] & 0xFF);
        
        // Continue analyzing
        System.out.println("\n【 Full Header Dump (first 96 bytes as structured data) 】");
        for (int offset = 0; offset < Math.min(96, objData.length); offset += 8) {
            System.out.printf("0x%02X: ", offset);
            for (int i = 0; i < 8 && offset + i < objData.length; i++) {
                System.out.printf("%02X ", objData[offset + i] & 0xFF);
            }
            System.out.print(" | ");
            
            // Try as little-endian values
            if (offset + 4 <= objData.length) {
                int le32 = (objData[offset] & 0xFF)
                    | ((objData[offset + 1] & 0xFF) << 8)
                    | ((objData[offset + 2] & 0xFF) << 16)
                    | ((objData[offset + 3] & 0xFF) << 24);
                System.out.printf("LE32=0x%08X ", le32);
            }
            
            System.out.println();
        }
        
        // Look for object start pattern
        System.out.println("\n【 Search for Object Records 】");
        System.out.println("Looking for potential object starts (MS+BS patterns)...\n");
        
        // The pattern might be: object size (MS) + type (BS)
        // Let's look for repeating patterns
        for (int i = 0x10; i < Math.min(0x100, objData.length); i += 0x10) {
            System.out.printf("0x%02X: ", i);
            for (int j = 0; j < 16 && i + j < objData.length; j++) {
                System.out.printf("%02X ", objData[i + j] & 0xFF);
            }
            System.out.println();
        }
    }
}
