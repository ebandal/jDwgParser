import io.dwg.api.DwgReader;
import io.dwg.api.DwgDocument;
import io.dwg.sections.handles.HandleRegistry;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.TreeMap;
import java.util.Map;

public class OffsetAnalyzer {
    public static void main(String[] args) throws Exception {
        String file = "samples/2004/Line.dwg";
        System.out.printf("\n=== Analyzing %s ===\n", file);
        byte[] data = Files.readAllBytes(Paths.get(file));
        DwgDocument doc = DwgReader.defaultReader().open(data);

        HandleRegistry reg = doc.handleRegistry();
        System.out.printf("Total handles: %d\n", reg.allHandles().size());

        // Group by handle ranges to see pattern
        TreeMap<Long, Long> sorted = new TreeMap<>();
        for (long h : reg.allHandles()) {
            sorted.put(h, reg.offsetFor(h).orElse(-1L));
        }

        System.out.println("\nAll handles and offsets:");
        int i = 0;
        for (Map.Entry<Long, Long> e : sorted.entrySet()) {
            if (i++ < 50) {  // Show first 50
                System.out.printf("  Handle 0x%X: offset %d\n", e.getKey(), e.getValue());
            }
        }
    }
}
