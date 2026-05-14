package debug;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.format.r2000.R2000FileStructureHandler;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class DebugR2000Offsets {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("samples/2000/Arc.dwg"));

        BitInput input = new ByteBufferBitInput(data);
        R2000FileStructureHandler handler = new R2000FileStructureHandler();
        FileHeaderFields header = handler.readHeader(input);

        System.out.println("=== R2000 Offsets Debug ===\n");

        Map<String, Long> offsets = header.sectionOffsets();
        Map<String, Long> sizes = header.sectionSizes();

        if (offsets == null) {
            System.out.println("offsets is NULL!");
        } else {
            System.out.printf("offsets has %d entries:\n", offsets.size());
            for (String key : offsets.keySet()) {
                System.out.printf("  %s: offset=0x%X, size=0x%X\n",
                    key, offsets.get(key), sizes.get(key));
            }
        }
    }
}
