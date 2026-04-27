package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersionDetector;
import io.dwg.format.common.DwgFileStructureHandlerFactory;
import io.dwg.format.common.DwgFileStructureHandler;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Debug R2007 section reading.
 */
public class DebugR2007Reading {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Debugging R2007 Section Reading");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        byte[] data = Files.readAllBytes(Paths.get("./samples/2007/Arc.dwg"));
        var version = DwgVersionDetector.detect(data);

        System.out.printf("File: Arc.dwg (%s)\n", version);
        System.out.printf("Size: %d bytes\n\n", data.length);

        try {
            BitInput input = new ByteBufferBitInput(data);
            DwgFileStructureHandler handler = DwgFileStructureHandlerFactory.forVersion(version);

            System.out.printf("Handler: %s\n", handler.getClass().getSimpleName());

            // Read header
            var header = handler.readHeader(input);
            System.out.printf("Header read OK\n");

            // Create fresh input for section reading (from byte 0)
            ByteBufferBitInput inputForSections = new ByteBufferBitInput(data);

            // Read sections
            Map<String, SectionInputStream> sections = handler.readSections(inputForSections, header);
            System.out.printf("Sections found: %d\n", sections.size());

            for (String name : sections.keySet()) {
                System.out.printf("  - %s\n", name);
            }

        } catch (Exception e) {
            System.out.printf("Error: %s\n", e.getMessage());
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
    }
}
