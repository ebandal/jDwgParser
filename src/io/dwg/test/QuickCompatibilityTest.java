package io.dwg.test;

import io.dwg.api.DwgReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.core.version.DwgVersionDetector;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class QuickCompatibilityTest {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  Phase 5: Quick Compatibility Test (After Object Classification Fix)");
        System.out.println("═══════════════════════════════════════════════════════════════\n");
        
        List<Path> dwgFiles = Files.walk(Paths.get("./samples"))
            .filter(p -> p.toString().endsWith(".dwg"))
            .collect(Collectors.toList());
        
        System.out.printf("Found %d DWG files\n\n", dwgFiles.size());
        
        Map<DwgVersion, Integer> versionSuccess = new TreeMap<>();
        Map<DwgVersion, Integer> versionTotal = new TreeMap<>();
        int totalSuccess = 0;
        
        for (Path file : dwgFiles) {
            try {
                byte[] data = Files.readAllBytes(file);
                DwgVersion v = DwgVersionDetector.detect(data);
                versionTotal.merge(v, 1, Integer::sum);
                
                try {
                    DwgReader.defaultReader().open(data);
                    versionSuccess.merge(v, 1, Integer::sum);
                    totalSuccess++;
                } catch (OutOfMemoryError | Exception e) {
                    // Failed
                }
            } catch (Exception e) {
                // Version detection failed
            }
        }
        
        System.out.println("───────────────────────────────────────────────────────────────");
        System.out.println("RESULTS BY VERSION");
        System.out.println("───────────────────────────────────────────────────────────────");
        for (DwgVersion v : versionTotal.keySet()) {
            int total = versionTotal.get(v);
            int success = versionSuccess.getOrDefault(v, 0);
            double rate = (success * 100.0) / total;
            System.out.printf("%-10s: %2d/%2d (%5.1f%%)%n", v, success, total, rate);
        }
        
        System.out.println("\n───────────────────────────────────────────────────────────────");
        System.out.println("OVERALL");
        System.out.println("───────────────────────────────────────────────────────────────");
        double overall = (totalSuccess * 100.0) / dwgFiles.size();
        System.out.printf("SUCCESS: %d/%d files (%.1f%%)%n", totalSuccess, dwgFiles.size(), overall);
        System.out.println("═══════════════════════════════════════════════════════════════");
    }
}
