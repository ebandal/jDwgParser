import java.nio.file.*;
import io.dwg.core.io.*;
import io.dwg.format.r2004.R2004FileStructureHandler;
import java.util.Map;

public class show_handles {
    public static void main(String[] args) throws Exception {
        Path file = Paths.get("samples/example_2004.dwg");
        byte[] data = Files.readAllBytes(file);
        
        io.dwg.core.version.DwgVersion version = 
            io.dwg.core.version.DwgVersionDetector.detect(data);
        
        BitInput input = new ByteBufferBitInput(data);
        R2004FileStructureHandler handler = new R2004FileStructureHandler();
        
        var headerFields = handler.readHeader(input);
        
        input = new ByteBufferBitInput(data);
        Map<String, SectionInputStream> sections = handler.readSections(input, headerFields);
        
        SectionInputStream handlesStream = sections.get("AcDb:Handles");
        if (handlesStream != null) {
            byte[] handles = handlesStream.rawBytes();
            System.out.printf("Handles section: %d bytes\n", handles.length);
            System.out.println("First 64 bytes (hex):");
            for (int i = 0; i < Math.min(64, handles.length); i += 16) {
                System.out.printf("  %04X: ", i);
                for (int j = 0; j < 16 && i + j < handles.length; j++) {
                    System.out.printf("%02X ", handles[i+j] & 0xFF);
                }
                System.out.println();
            }
        }
    }
}
