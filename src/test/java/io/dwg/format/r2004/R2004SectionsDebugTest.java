package io.dwg.format.r2004;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.core.io.SectionInputStream;
import io.dwg.format.common.DwgFileStructureHandler;
import io.dwg.format.common.DwgFileStructureHandlerFactory;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.core.version.DwgVersion;
import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Debug R2004 section reading to understand structure
 */
public class R2004SectionsDebugTest {
    public static void main(String[] args) throws Exception {
        System.out.println("=".repeat(80));
        System.out.println("R2004 Sections Debug Test");
        System.out.println("=".repeat(80));
        System.out.println();

        String filePath = "samples/2004/Arc.dwg";
        byte[] data = Files.readAllBytes(Paths.get(filePath));

        DwgVersion version = DwgVersion.R2004;
        DwgFileStructureHandler handler = DwgFileStructureHandlerFactory.forVersion(version);

        System.out.println("Step 1: Reading header");
        BitInput input = new ByteBufferBitInput(data);
        FileHeaderFields headerFields = handler.readHeader(input);
        System.out.println("  ✓ Header read completed");
        System.out.println();

        System.out.println("Step 2: Reading sections");
        input = new ByteBufferBitInput(data);
        Map<String, SectionInputStream> sections = handler.readSections(input, headerFields);
        System.out.println("  ✓ Sections read completed");
        System.out.println("  Section count: " + sections.size());
        System.out.println();

        System.out.println("Sections found:");
        System.out.println("-".repeat(80));
        for (String name : sections.keySet()) {
            SectionInputStream stream = sections.get(name);
            System.out.printf("  %-30s: %10d bytes\n", name, stream.size());
        }

        System.out.println();
        System.out.println("Step 3: Parse with DwgReader");
        DwgDocument doc = DwgReader.defaultReader().open(Paths.get(filePath));
        System.out.println("  ✓ DwgReader completed");
        System.out.println("  Version: " + doc.version());
        System.out.println("  ObjectMap size: " + doc.objectMap().size());
        System.out.println("  Entities count: " + doc.entities().size());

        System.out.println();
        System.out.println("=".repeat(80));
    }
}
