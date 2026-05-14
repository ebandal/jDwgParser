import io.dwg.api.DwgReader;
import io.dwg.api.DwgDocument;
import io.dwg.sections.handles.HandleRegistry;
import java.nio.file.Paths;
import java.nio.file.Files;

public class DumpOffsets {
    public static void main(String[] args) throws Exception {
        String file = "samples/2004/Line.dwg";
        byte[] data = Files.readAllBytes(Paths.get(file));
        DwgDocument doc = DwgReader.defaultReader().open(data);

        HandleRegistry reg = doc.handleRegistry();
        byte[] objData = null;

        // We need to extract Objects section data
        // For now, just show handle offsets and ask what's at offset 4

        System.out.println("Analyzing first few handles in 2004/Line.dwg");
        System.out.println("Objects section size would be: 26784 bytes\n");

        System.out.println("First 10 in-range handle offsets:");
        int count = 0;
        for (long h : reg.allHandles()) {
            long offset = reg.offsetFor(h).orElse(-1L);
            if (offset >= 0 && offset < 26784 && count++ < 10) {
                System.out.printf("  Handle 0x%X: offset %d (0x%X)\n", h, offset, offset);
            }
        }
    }
}
