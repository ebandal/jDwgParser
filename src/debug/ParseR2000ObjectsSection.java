package debug;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.format.r2000.R2000FileStructureHandler;
import io.dwg.sections.classes.ClassesSectionParser;
import io.dwg.sections.classes.DwgClassDefinition;
import io.dwg.sections.handles.HandleRegistry;
import io.dwg.sections.handles.HandlesSectionParser;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class ParseR2000ObjectsSection {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("samples/2000/Arc.dwg"));

        BitInput input = new ByteBufferBitInput(data);
        R2000FileStructureHandler handler = new R2000FileStructureHandler();
        FileHeaderFields header = handler.readHeader(input);

        // Read sections to get Objects data
        input = new ByteBufferBitInput(data);
        Map<String, SectionInputStream> sections = handler.readSections(input, header);

        SectionInputStream objectsSection = sections.get("AcDb:AcDbObjects");
        if (objectsSection == null) {
            System.out.println("Objects section not found!");
            return;
        }

        byte[] objectsData = objectsSection.rawBytes();
        System.out.println("=== Parsing R2000 Objects Section ===\n");
        System.out.printf("Objects section size: 0x%X (%d bytes)\n\n", objectsData.length, objectsData.length);

        // Try to parse as Classes section
        System.out.println("【 Attempt 1: Parse as Classes Section 】");
        tryParseClasses(objectsData);

        // Try to parse as Handles section
        System.out.println("\n【 Attempt 2: Parse as Handles Section 】");
        tryParseHandles(objectsData);

        // Try to parse as Objects stream
        System.out.println("\n【 Attempt 3: Parse as Objects Stream 】");
        tryParseObjects(objectsData);

        // Look for section boundaries
        System.out.println("\n【 Attempt 4: Find Embedded Sections 】");
        findEmbeddedSections(objectsData);
    }

    static void tryParseClasses(byte[] data) {
        try {
            SectionInputStream stream = new SectionInputStream(data, "AcDb:Classes");
            ClassesSectionParser parser = new ClassesSectionParser();
            List<DwgClassDefinition> classes = parser.parse(stream, DwgVersion.R2000);
            System.out.printf("✓ Classes parsed: %d class definitions\n", classes.size());
            for (int i = 0; i < Math.min(5, classes.size()); i++) {
                DwgClassDefinition cls = classes.get(i);
                System.out.printf("  [%d] %s (type=%d)\n",
                    i, cls.dxfRecordName(), cls.classNumber());
            }
        } catch (Exception e) {
            System.out.printf("✗ Classes parsing failed: %s\n", e.getMessage());
        }
    }

    static void tryParseHandles(byte[] data) {
        try {
            SectionInputStream stream = new SectionInputStream(data, "AcDb:Handles");
            HandlesSectionParser parser = new HandlesSectionParser();
            HandleRegistry handles = parser.parse(stream, DwgVersion.R2000);
            System.out.printf("✓ Handles parsed: %d handle entries\n", handles.allHandles().size());
            var sample = handles.sortedEntries().stream().limit(5).toList();
            for (var entry : sample) {
                System.out.printf("  Handle 0x%X -> offset 0x%X\n", entry.handle(), entry.offset());
            }
        } catch (Exception e) {
            System.out.printf("✗ Handles parsing failed: %s\n", e.getMessage());
        }
    }

    static void tryParseObjects(byte[] data) {
        try {
            ByteBufferBitInput bitInput = new ByteBufferBitInput(ByteBuffer.wrap(data));
            BitStreamReader reader = new BitStreamReader(bitInput, DwgVersion.R2000);

            System.out.println("Trying to read first few values from Objects stream:");
            for (int i = 0; i < 10; i++) {
                try {
                    int ms = reader.readModularShort();
                    int bs = reader.readBitShort();
                    System.out.printf("  [%d] MS=%d, BS=%d\n", i, ms, bs);
                } catch (Exception e) {
                    System.out.printf("  [%d] Failed: %s\n", i, e.getMessage());
                    break;
                }
            }
        } catch (Exception e) {
            System.out.printf("✗ Objects stream parsing failed: %s\n", e.getMessage());
        }
    }

    static void findEmbeddedSections(byte[] data) {
        // Look for Classes START_SENTINEL: 8D A1 C4 B8 C4 A9 F8 C5 C0 DC F4 5F E7 CF B6 8A
        byte[] classesSentinel = {
            (byte)0x8D, (byte)0xA1, (byte)0xC4, (byte)0xB8, (byte)0xC4, (byte)0xA9,
            (byte)0xF8, (byte)0xC5, (byte)0xC0, (byte)0xDC, (byte)0xF4, (byte)0x5F,
            (byte)0xE7, (byte)0xCF, (byte)0xB6, (byte)0x8A
        };

        System.out.println("Looking for Classes sentinel...");
        for (int i = 0; i <= data.length - classesSentinel.length; i++) {
            boolean found = true;
            for (int j = 0; j < classesSentinel.length; j++) {
                if (data[i + j] != classesSentinel[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                System.out.printf("✓ Classes sentinel found at offset 0x%X\n", i);
                // Try to read section size
                if (i + 20 < data.length) {
                    int size = readLE32(data, i + 16);
                    System.out.printf("  Section size (next RL): 0x%X (%d bytes)\n", size, size);
                }
            }
        }

        // Look for specific Handles pattern (UMC/MC encoded)
        System.out.println("\nLooking for Handles pattern (pages with handle deltas)...");
        System.out.println("(This is harder to identify without more context)");
    }

    static int readLE32(byte[] data, int offset) {
        if (offset + 4 > data.length) return 0;
        return (data[offset] & 0xFF)
            | ((data[offset + 1] & 0xFF) << 8)
            | ((data[offset + 2] & 0xFF) << 16)
            | ((data[offset + 3] & 0xFF) << 24);
    }
}
