package run;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Shows entity and object counts per file and version summary.
 */
public class EntityCountTester {
    public static void main(String[] args) throws Exception {
        Path samplesDir = Paths.get("samples");
        List<Path> dwgFiles = new ArrayList<>();
        try (Stream<Path> files = Files.walk(samplesDir)) {
            files.filter(p -> p.toString().toLowerCase().endsWith(".dwg"))
                 .sorted()
                 .forEach(dwgFiles::add);
        }

        int totalFiles = 0, passedFiles = 0, zeroEntityFiles = 0;
        long totalEntities = 0, totalObjects = 0;
        Map<String, long[]> versionStats = new TreeMap<>();

        System.out.printf("%-45s %-8s %7s %7s%n", "File", "Version", "Entities", "Objects");
        System.out.println("-".repeat(72));

        for (Path p : dwgFiles) {
            totalFiles++;
            try {
                DwgDocument doc = DwgReader.defaultReader().open(p);
                int entities = doc.entities().size();
                int objects = doc.objectMap().size();
                String version = doc.version().toString();
                passedFiles++;
                totalEntities += entities;
                totalObjects += objects;
                if (entities == 0) zeroEntityFiles++;
                versionStats.computeIfAbsent(version, k -> new long[4]);
                long[] s = versionStats.get(version);
                s[0]++;
                s[1] += entities;
                s[2] += objects;
                if (entities > 0) s[3]++;
                System.out.printf("%-45s %-8s %7d %7d%n",
                    p.getFileName(), version, entities, objects);
            } catch (Exception e) {
                System.out.printf("%-45s FAIL: %s%n", p.getFileName(), e.getMessage());
            }
        }

        System.out.println("\n" + "=".repeat(72));
        System.out.println("SUMMARY");
        System.out.println("=".repeat(72));
        System.out.printf("Total files:          %d%n", totalFiles);
        System.out.printf("Passed:               %d (%.1f%%)%n", passedFiles, passedFiles * 100.0 / totalFiles);
        System.out.printf("Total entities:       %d%n", totalEntities);
        System.out.printf("Total objects:        %d%n", totalObjects);
        System.out.printf("Files with 0 entities:%d%n", zeroEntityFiles);
        System.out.println();
        System.out.printf("%-10s %6s %10s %10s %12s%n", "Version", "Files", "Entities", "Objects", ">0 entities");
        System.out.println("-".repeat(55));
        for (Map.Entry<String, long[]> e : versionStats.entrySet()) {
            long[] s = e.getValue();
            System.out.printf("%-10s %6d %10d %10d %12d%n",
                e.getKey(), s[0], s[1], s[2], s[3]);
        }
    }
}
