package run;

import io.dwg.api.DwgReader;
import io.dwg.api.DwgDocument;
import io.dwg.core.version.DwgVersion;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Quick test of all DWG sample files
 */
public class QuickSampleTester {
    public static void main(String[] args) throws Exception {
        Map<String, Integer> passed = new LinkedHashMap<>();
        Map<String, Integer> total = new LinkedHashMap<>();
        Map<String, List<String>> failures = new LinkedHashMap<>();

        Path samplesDir = Paths.get("samples");
        Files.walk(samplesDir)
            .filter(p -> p.toString().endsWith(".dwg"))
            .sorted()
            .forEach(filePath -> {
                try {
                    DwgDocument doc = DwgReader.defaultReader().open(filePath);
                    String version = doc.version().toString();
                    passed.put(version, passed.getOrDefault(version, 0) + 1);
                    total.put(version, total.getOrDefault(version, 0) + 1);
                } catch (Exception e) {
                    String version = extractVersion(filePath);
                    total.put(version, total.getOrDefault(version, 0) + 1);
                    failures.computeIfAbsent(version, k -> new ArrayList<>()).add(filePath.getFileName().toString());
                }
            });

        System.out.println("\n=== DWG Sample File Test Results ===\n");
        int totalPass = 0, totalCount = 0;
        for (String v : new String[]{"R13", "R14", "R2000", "R2004", "R2007", "R2010", "R2013", "R2018"}) {
            int p = passed.getOrDefault(v, 0);
            int t = total.getOrDefault(v, 0);
            totalPass += p;
            totalCount += t;
            if (t > 0) {
                System.out.printf("%s: %d/%d (%.0f%%)\n", v, p, t, 100.0 * p / t);
            }
        }
        System.out.printf("\nTotal: %d/%d (%.1f%%)\n", totalPass, totalCount, 100.0 * totalPass / totalCount);

        // 추가: R2004 샘플에서 entity 개수 확인
        System.out.println("\n=== R2004 Entity Count Analysis ===\n");
        Path r2004Dir = Paths.get("samples");
        Files.walk(r2004Dir)
            .filter(p -> p.toString().contains("2004") && p.toString().endsWith(".dwg"))
            .sorted()
            .limit(3)
            .forEach(filePath -> {
                try {
                    DwgDocument doc = DwgReader.defaultReader().open(filePath);
                    int entityCount = doc.objectMap() != null ? doc.objectMap().size() : 0;
                    System.out.printf("%s: %d entities\n", filePath.getFileName(), entityCount);
                } catch (Exception e) {
                    System.out.printf("%s: ERROR\n", filePath.getFileName());
                }
            });
    }

    static String extractVersion(Path p) {
        String path = p.toString();
        if (path.contains("2018")) return "R2018";
        if (path.contains("2013")) return "R2013";
        if (path.contains("2010")) return "R2010";
        if (path.contains("2007")) return "R2007";
        if (path.contains("2004")) return "R2004";
        if (path.contains("2000")) return "R2000";
        if (path.contains("14")) return "R14";
        if (path.contains("13")) return "R13";
        return "Unknown";
    }
}
