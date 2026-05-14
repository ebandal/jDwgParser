import java.nio.file.*;
import io.dwg.api.DwgReader;
import io.dwg.api.DwgDocument;

public class test_example {
    public static void main(String[] args) throws Exception {
        Path file = Paths.get("samples/example_2004.dwg");
        System.out.printf("Testing: %s (%,d bytes)\n", file, Files.size(file));
        
        DwgDocument doc = DwgReader.defaultReader().open(file);
        
        System.out.printf("Entities found: %d\n", doc.entities().size());
        for (int i = 0; i < Math.min(10, doc.entities().size()); i++) {
            var entity = doc.entities().get(i);
            System.out.printf("  %d: %s\n", i, entity.getClass().getSimpleName());
        }
    }
}
