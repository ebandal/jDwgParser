package io.dwg.test;

import io.dwg.api.DwgReader;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Analyzes what UNKNOWN object types are parsed.
 * Run with DEBUG logging enabled to see detailed output.
 */
public class AnalyzeUnknownTypes {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Analyzing Unknown Object Types in R2000 Arc.dwg");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("\nEnable -Ddebug=true to see full debug output with type codes\n");

        byte[] data = Files.readAllBytes(Paths.get("./samples/2000/Arc.dwg"));

        try {
            var doc = DwgReader.defaultReader().open(data);
            System.out.printf("Parsed document: %d handles, %d objects\n",
                doc.handleRegistry().size(), doc.objectMap().size());

            System.out.println("\nFor detailed analysis, look for 'UNKNOWN' or 'FAIL-NoReader' in debug output above");
            System.out.println("Type codes will be shown as 'typeCode' or 'type=' values");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("═══════════════════════════════════════════════════════════════");
    }
}
