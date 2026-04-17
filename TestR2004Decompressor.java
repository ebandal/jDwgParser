import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersionDetector;
import io.dwg.format.common.DwgFileStructureHandler;
import io.dwg.format.common.DwgFileStructureHandlerFactory;
import io.dwg.format.common.FileHeaderFields;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class TestR2004Decompressor {
    public static void main(String[] args) throws Exception {
        String filePath = "samples/2004/Arc.dwg";
        System.out.println("Testing R2004Lz77Decompressor on: " + filePath);

        byte[] data = Files.readAllBytes(Paths.get(filePath));

        // Detect version
        var version = DwgVersionDetector.detect(data);
        System.out.println("DWG Version: " + version);

        // Get handler
        DwgFileStructureHandler handler = DwgFileStructureHandlerFactory.forVersion(version);
        BitInput input = new ByteBufferBitInput(data);

        // Read header
        FileHeaderFields headerFields = handler.readHeader(input);
        System.out.println("Header parsed successfully");

        // Create fresh input and read sections
        input = new ByteBufferBitInput(data);

        try {
            Map<String, SectionInputStream> sections = handler.readSections(input, headerFields);
            System.out.println("\n✅ R2004FileStructureHandler.readSections() completed successfully");
            System.out.println("Sections extracted and decompressed:");
            for (String sectionName : sections.keySet()) {
                SectionInputStream si = sections.get(sectionName);
                System.out.printf("  - %s (%d bytes)\n", sectionName, si.size());
            }
        } catch (Exception e) {
            System.out.println("\n❌ Error reading sections: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
