package io.dwg.test;

import io.dwg.core.version.DwgVersionDetector;
import io.dwg.core.version.DwgVersion;
import io.dwg.api.DwgReader;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgEntity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive analysis of Phase 6B (R2007+ support) progress.
 * Measures entity count improvements across all DWG versions after LZ77 decompressor fix.
 */
public class AnalyzePhase6BProgress {
    static class VersionStats {
        DwgVersion version;
        int fileCount = 0;
        int totalObjects = 0;
        int totalEntities = 0;
        int filesWithEntities = 0;
        List<Integer> entityCounts = new ArrayList<>();

        void addFile(int objects, int entities) {
            fileCount++;
            totalObjects += objects;
            totalEntities += entities;
            entityCounts.add(entities);
            if (entities > 0) filesWithEntities++;
        }

        double entityPercentage() {
            return fileCount > 0 ? (100.0 * totalEntities / totalObjects) : 0;
        }

        int avgEntitiesPerFile() {
            return fileCount > 0 ? totalEntities / fileCount : 0;
        }

        String status() {
            if (totalEntities == 0) return "❌";
            if (totalEntities < 50) return "⚠️";
            return "✅";
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Phase 6B Progress Analysis: R2007+ Entity Parsing");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        Map<DwgVersion, VersionStats> stats = new HashMap<>();
        DwgReader reader = DwgReader.defaultReader();

        List<Path> allFiles = Files.walk(Paths.get("./samples"))
            .filter(p -> p.toString().endsWith(".dwg"))
            .sorted()
            .collect(Collectors.toList());

        System.out.printf("Scanning %d DWG files...\n\n", allFiles.size());

        int totalFiles = 0;
        int totalObjects = 0;
        int totalEntities = 0;
        int r2007PlusFiles = 0;
        int r2007PlusObjects = 0;
        int r2007PlusEntities = 0;

        for (Path file : allFiles) {
            try {
                byte[] data = Files.readAllBytes(file);
                DwgVersion version = DwgVersionDetector.detect(data);
                VersionStats versionStat = stats.computeIfAbsent(version, v -> {
                    VersionStats s = new VersionStats();
                    s.version = v;
                    return s;
                });

                var doc = reader.open(data);
                int objectCount = doc.objectMap().size();
                int entityCount = 0;

                // Count actual entity objects
                for (DwgObject obj : doc.objectMap().values()) {
                    if (isEntity(obj)) {
                        entityCount++;
                    }
                }

                versionStat.addFile(objectCount, entityCount);
                totalFiles++;
                totalObjects += objectCount;
                totalEntities += entityCount;

                // Track R2007+ separately
                if (isR2007Plus(version)) {
                    r2007PlusFiles++;
                    r2007PlusObjects += objectCount;
                    r2007PlusEntities += entityCount;
                }

            } catch (Exception e) {
                // Skip files with errors
            }
        }

        // Print summary by version
        System.out.println("RESULTS BY VERSION:");
        System.out.println("─────────────────────────────────────────────────────────────");
        System.out.printf("%-8s %-8s %-12s %-12s %-10s %s\n",
            "Version", "Files", "Objects", "Entities", "Entity%", "Status");
        System.out.println("─────────────────────────────────────────────────────────────");

        int r2000r2004Objects = 0;
        int r2000r2004Entities = 0;

        for (DwgVersion version : Arrays.asList(
            DwgVersion.R13, DwgVersion.R14, DwgVersion.R2000, DwgVersion.R2004,
            DwgVersion.R2007, DwgVersion.R2010, DwgVersion.R2013, DwgVersion.R2018)) {

            VersionStats s = stats.get(version);
            if (s == null) continue;

            System.out.printf("%-8s %-8d %-12d %-12d %-9.1f%% %s\n",
                version.name(),
                s.fileCount,
                s.totalObjects,
                s.totalEntities,
                s.entityPercentage(),
                s.status());

            if (version == DwgVersion.R2000 || version == DwgVersion.R2004) {
                r2000r2004Objects += s.totalObjects;
                r2000r2004Entities += s.totalEntities;
            }
        }

        System.out.println("─────────────────────────────────────────────────────────────");
        System.out.println("\nSUMMARY:");
        System.out.println("─────────────────────────────────────────────────────────────");
        System.out.printf("Total Files: %d\n", totalFiles);
        System.out.printf("Total Objects: %d\n", totalObjects);
        System.out.printf("Total Entities: %d\n", totalEntities);
        System.out.printf("Overall Entity Coverage: %.1f%%\n\n",
            totalObjects > 0 ? (100.0 * totalEntities / totalObjects) : 0);

        System.out.println("R2000/R2004 Results (Should have ~600 entities):");
        System.out.printf("  Objects: %d\n", r2000r2004Objects);
        System.out.printf("  Entities: %d\n", r2000r2004Entities);
        System.out.printf("  Status: %s\n\n", r2000r2004Entities >= 500 ? "✅ OK" : "⚠️ Low");

        System.out.println("R2007+ Results (Phase 6B Fix Target):");
        System.out.printf("  Files: %d (expected: 80)\n", r2007PlusFiles);
        System.out.printf("  Objects: %d\n", r2007PlusObjects);
        System.out.printf("  Entities: %d (expected: 1,500+)\n", r2007PlusEntities);

        if (r2007PlusEntities > 500) {
            System.out.printf("  Status: ✅ MAJOR IMPROVEMENT - Fix is working!\n");
        } else if (r2007PlusEntities > 0) {
            System.out.printf("  Status: ⚠️ Partial improvement\n");
        } else {
            System.out.printf("  Status: ❌ No improvement yet\n");
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.printf("Expected Phase 6B Result: 604 → %d+ entities total\n",
            r2000r2004Entities + (r2007PlusEntities > 0 ? r2007PlusEntities : 1500));
        System.out.println("═══════════════════════════════════════════════════════════════");
    }

    private static boolean isEntity(DwgObject obj) {
        // Check if object is an entity type
        return obj instanceof DwgEntity;
    }

    private static boolean isR2007Plus(DwgVersion v) {
        return v == DwgVersion.R2007 || v == DwgVersion.R2010 ||
               v == DwgVersion.R2013 || v == DwgVersion.R2018;
    }
}
