package io.dwg.test;

import io.dwg.core.version.DwgVersionDetector;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyze why R2007+ files have 0 objects.
 */
public class AnalyzeR2007Structure {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Analyzing R2007+ Objects Issue");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        List<Path> r2007Files = Files.walk(Paths.get("./samples"))
            .filter(p -> p.toString().endsWith(".dwg"))
            .filter(p -> {
                try {
                    var v = DwgVersionDetector.detect(Files.readAllBytes(p));
                    String vstr = v.toString();
                    return vstr.contains("2007") || vstr.contains("2010") || vstr.contains("2013");
                } catch (Exception e) {
                    return false;
                }
            })
            .sorted()
            .collect(Collectors.toList());

        System.out.printf("Found %d R2007+ files\n\n", r2007Files.size());

        for (Path file : r2007Files.stream().limit(3).collect(Collectors.toList())) {
            try {
                byte[] data = Files.readAllBytes(file);
                var version = DwgVersionDetector.detect(data);

                System.out.printf("File: %s (%s)\n", file.getFileName(), version);
                System.out.printf("Size: %d bytes\n", data.length);

                var doc = io.dwg.api.DwgReader.defaultReader().open(data);
                System.out.printf("Objects parsed: %d\n", doc.objectMap().size());
                System.out.printf("Handles: %d\n", doc.handleRegistry().size());
                System.out.printf("Result: %s\n\n", doc.objectMap().isEmpty() ? "❌ EMPTY" : "✅ OK");

            } catch (Exception e) {
                System.out.printf("Error: %s\n\n", e.getMessage());
            }
        }

        System.out.println("═══════════════════════════════════════════════════════════════");
    }
}
