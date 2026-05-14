package io.dwg.test;

import io.dwg.api.DwgReader;
import io.dwg.core.version.DwgVersionDetector;
import io.dwg.entities.DwgEntity;
import io.dwg.entities.DwgObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyze entity parsing across multiple DWG files.
 */
public class AnalyzeMultipleFiles {
    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Entity Analysis - Multiple Files");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        List<Path> files = Files.walk(Paths.get("./samples"))
            .filter(p -> p.toString().endsWith(".dwg"))
            .sorted()
            .collect(Collectors.toList());

        Map<String, Statistics> stats = new TreeMap<>();

        for (Path file : files) {
            try {
                byte[] data = Files.readAllBytes(file);
                var version = DwgVersionDetector.detect(data);
                var doc = DwgReader.defaultReader().open(data);

                int entities = 0;
                int nonEntities = 0;
                Set<String> entityTypes = new HashSet<>();

                for (DwgObject obj : doc.objectMap().values()) {
                    if (obj instanceof DwgEntity) {
                        entities++;
                        entityTypes.add(obj.objectType().toString());
                    } else {
                        nonEntities++;
                    }
                }

                String versionStr = version.toString();
                if (!stats.containsKey(versionStr)) {
                    stats.put(versionStr, new Statistics());
                }
                Statistics s = stats.get(versionStr);
                s.totalFiles++;
                s.totalObjects += doc.objectMap().size();
                s.totalEntities += entities;
                s.totalNonEntities += nonEntities;
                s.totalEntityTypes.addAll(entityTypes);
            } catch (Exception e) {
                // Skip failed files
            }
        }

        System.out.println("Summary by Version:");
        System.out.println("Version   Files  Objects Entities NonEnt  EntityTypes");
        System.out.println("────────────────────────────────────────────────────");

        stats.forEach((version, s) -> {
            System.out.printf("%-8s %5d  %7d %8d %6d  %s\n",
                version,
                s.totalFiles,
                s.totalObjects,
                s.totalEntities,
                s.totalNonEntities,
                String.join(",", s.totalEntityTypes.stream().sorted().limit(3).collect(Collectors.toList())));
        });

        int totalEntities = stats.values().stream().mapToInt(s -> s.totalEntities).sum();
        int totalObjects = stats.values().stream().mapToInt(s -> s.totalObjects).sum();

        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.printf("Total: %d objects, %d entities (%.1f%%)\n",
            totalObjects, totalEntities, (100.0 * totalEntities / totalObjects));
    }

    static class Statistics {
        int totalFiles;
        int totalObjects;
        int totalEntities;
        int totalNonEntities;
        Set<String> totalEntityTypes = new HashSet<>();
    }
}
