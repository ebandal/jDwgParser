package debug;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.format.common.DwgFileStructureHandler;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.core.io.SectionInputStream;
import io.dwg.format.r2000.R2000FileStructureHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class AnalyzeMultipleR2000 {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Analysis of Multiple R2000 Files ===\n");
        
        String[] files = {"Arc.dwg", "Helix.dwg", "Leader.dwg", "Constraints.dwg"};
        
        for (String file : files) {
            Path path = Paths.get("samples/2000/" + file);
            if (Files.exists(path)) {
                analyzeFile(path);
                System.out.println();
            }
        }
    }
    
    static void analyzeFile(Path path) throws Exception {
        byte[] data = Files.readAllBytes(path);
        
        BitInput input = new ByteBufferBitInput(data);
        R2000FileStructureHandler handler = new R2000FileStructureHandler();
        FileHeaderFields header = handler.readHeader(input);
        
        input = new ByteBufferBitInput(data);
        Map<String, SectionInputStream> sections = handler.readSections(input, header);
        
        SectionInputStream objectsSection = sections.get("AcDb:AcDbObjects");
        if (objectsSection == null) {
            System.out.println(path.getFileName() + ": Objects section not found");
            return;
        }
        
        byte[] objData = objectsSection.rawBytes();
        
        // Count zeros
        int zeroCount = 0;
        int nonZeroCount = 0;
        for (byte b : objData) {
            if (b == 0) zeroCount++;
            else nonZeroCount++;
        }
        
        System.out.printf("%s: %d bytes (%.1f%% zeros, %.1f%% non-zero)\n",
            path.getFileName(), objData.length,
            100.0 * zeroCount / objData.length,
            100.0 * nonZeroCount / objData.length);
        
        // Show first 96 bytes
        System.out.print("  First 96 bytes: ");
        for (int i = 0; i < Math.min(96, objData.length); i++) {
            if (i > 0 && i % 16 == 0) System.out.print("\n                 ");
            System.out.printf("%02X ", objData[i] & 0xFF);
        }
        System.out.println();
    }
}
