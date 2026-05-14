package debug;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.version.DwgVersion;
import io.dwg.core.version.DwgVersionDetector;
import io.dwg.format.common.DwgFileStructureHandler;
import io.dwg.format.common.DwgFileStructureHandlerFactory;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.core.io.SectionInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class CompareR2000vsR2004 {
    public static void main(String[] args) throws Exception {
        System.out.println("=== R2000 vs R2004 Objects Section Comparison ===\n");
        
        analyzeFile("samples/2000/Arc.dwg", "R2000");
        System.out.println("\n" + "=".repeat(70) + "\n");
        analyzeFile("samples/2004/circle.dwg", "R2004");
    }
    
    static void analyzeFile(String path, String label) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get(path));
        DwgVersion version = DwgVersionDetector.detect(data);
        
        BitInput input = new ByteBufferBitInput(data);
        DwgFileStructureHandler handler = DwgFileStructureHandlerFactory.forVersion(version);
        FileHeaderFields header = handler.readHeader(input);
        
        input = new ByteBufferBitInput(data);
        Map<String, SectionInputStream> sections = handler.readSections(input, header);
        
        SectionInputStream objectsSection = sections.get("AcDb:AcDbObjects");
        if (objectsSection == null) {
            objectsSection = sections.get("AcDb:Objects");
        }
        
        if (objectsSection == null) {
            System.out.println(label + ": Objects section not found");
            return;
        }
        
        byte[] objData = objectsSection.rawBytes();
        System.out.printf("[%s] Objects section: 0x%X bytes (%d)\n", label, objData.length, objData.length);
        
        // Byte distribution
        int zeroCount = 0;
        int[] freq = new int[256];
        for (byte b : objData) {
            if (b == 0) zeroCount++;
            freq[b & 0xFF]++;
        }
        
        System.out.printf("  Zero bytes: %d (%.1f%%)\n", zeroCount, 100.0 * zeroCount / objData.length);
        
        System.out.println("  Most common bytes:");
        for (int i = 0; i < 256; i++) {
            if (freq[i] > objData.length / 50) {  // Top 2%
                System.out.printf("    0x%02X: %d (%.1f%%)\n", i, freq[i], 100.0 * freq[i] / objData.length);
            }
        }
        
        // First 128 bytes hex dump
        System.out.println("\n  First 128 bytes (hex):");
        for (int i = 0; i < Math.min(128, objData.length); i += 16) {
            System.out.printf("    0x%04X: ", i);
            for (int j = 0; j < 16 && i + j < objData.length; j++) {
                System.out.printf("%02X ", objData[i + j] & 0xFF);
            }
            System.out.println();
        }
    }
}
