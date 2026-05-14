import java.nio.file.*;
import io.dwg.core.io.*;
import io.dwg.format.r2004.R2004FileStructureHandler;

public class test_section3 {
    public static void main(String[] args) throws Exception {
        // Read the Classes section data that was extracted
        // Section 3: 2688 bytes
        
        // We need to manually extract section 3 from example_2004.dwg
        // For now, let's just check the DwgReader and see what sections it can parse
        
        Path file = Paths.get("samples/example_2004.dwg");
        io.dwg.api.DwgReader reader = io.dwg.api.DwgReader.defaultReader();
        
        try {
            io.dwg.api.DwgDocument doc = reader.open(file);
            System.out.println("Sections parsed successfully!");
            System.out.println("Entities: " + doc.entities().size());
        } catch (Exception e) {
            System.out.println("Parse failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
