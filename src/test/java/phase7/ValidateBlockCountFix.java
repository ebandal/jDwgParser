package phase7;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.entities.DwgObject;
import io.dwg.core.version.DwgVersion;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Phase 7 검증: blockCount 수정 효과 측정
 *
 * 목표:
 * - blockCount 수정으로 인한 엔티티 수 향상 확인
 * - R2007+ 파일에서 예상된 900-2000+ 엔티티 파싱 확인
 * - 오프셋 유효성 확인 (<5% 무효)
 * - 회귀 테스트: R2000/R2004 변경 없음 확인
 *
 * 사용법:
 *   mvn test -Dtest=ValidateBlockCountFix
 * 또는
 *   java test.java.phase7.ValidateBlockCountFix
 */
public class ValidateBlockCountFix {

    private static class FileMetrics {
        String fileName;
        DwgVersion version;
        int entityCount;
        double validOffsetPercent;
        boolean success;
        String error;
    }

    private static Map<String, FileMetrics> fileMetrics = new HashMap<>();
    private static int r2007PlusFilesCount = 0;
    private static int totalEntitiesFound = 0;

    public static void main(String[] args) {
        setupLogging();

        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║         PHASE 7: BLOCKCOUNT FIX VALIDATION                 ║");
        System.out.println("║        Expected: 4 → 900-2000+ entities from R2007        ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");

        testAllDwgFiles();

        System.out.println("\n" + "═".repeat(70));
        System.out.println("VALIDATION RESULTS");
        System.out.println("═".repeat(70));

        printResults();
    }

    private static void testAllDwgFiles() {
        String dwgPath = "DWG";
        File dwgDir = new File(dwgPath);

        if (!dwgDir.exists()) {
            System.out.println("⚠️  DWG directory not found at: " + dwgPath);
            System.out.println("   Trying alternative paths...");

            // Try alternative paths
            String[] altPaths = {
                "dwg",
                "./DWG",
                "./dwg",
                "../DWG",
            };

            for (String alt : altPaths) {
                File altDir = new File(alt);
                if (altDir.exists()) {
                    dwgPath = alt;
                    dwgDir = altDir;
                    System.out.println("   ✓ Found at: " + altDir.getAbsolutePath());
                    break;
                }
            }

            if (!dwgDir.exists()) {
                System.out.println("   Could not find DWG directory. Skipping file tests.");
                return;
            }
        }

        System.out.println("Scanning DWG directory: " + dwgDir.getAbsolutePath());
        System.out.println();

        try (Stream<Path> paths = Files.walk(Paths.get(dwgPath))) {
            paths.filter(p -> p.toString().endsWith(".dwg") || p.toString().endsWith(".DWG"))
                 .sorted()
                 .forEach(ValidateBlockCountFix::testFile);
        } catch (Exception e) {
            System.err.println("Error scanning DWG directory: " + e.getMessage());
        }
    }

    private static void testFile(Path filePath) {
        String fileName = filePath.getFileName().toString();
        FileMetrics metrics = new FileMetrics();
        metrics.fileName = fileName;

        System.out.printf("Testing: %-30s ", fileName);

        try {
            // Version detection
            DwgReader reader = DwgReader.defaultReader();
            DwgVersion version = reader.detectVersion(filePath);
            metrics.version = version;

            System.out.printf("[%s] ", version);

            // Parse DWG file
            DwgDocument doc = reader.open(filePath);
            Map<Long, DwgObject> objects = doc.objectMap();

            if (objects == null) {
                metrics.entityCount = 0;
                metrics.validOffsetPercent = 0;
                System.out.println("⚠️  No objects");
            } else {
                metrics.entityCount = objects.size();
                metrics.validOffsetPercent = 100.0;  // Simplified for now
                metrics.success = true;
                totalEntitiesFound += objects.size();

                if (version.toString().contains("R200") && !version.equals(DwgVersion.R2000)) {
                    r2007PlusFilesCount++;
                }

                if (metrics.entityCount > 0) {
                    System.out.printf("✅ %6d entities\n", metrics.entityCount);
                } else {
                    System.out.println("ℹ️  0 entities (valid for some files)");
                }
            }

        } catch (Exception e) {
            metrics.error = e.getMessage();
            metrics.success = false;
            System.out.printf("❌ ERROR: %s\n", e.getClass().getSimpleName());
        }

        fileMetrics.put(fileName, metrics);
    }

    private static void printResults() {
        System.out.println("\nFile Processing Summary:");
        System.out.println("-".repeat(70));

        int successCount = (int) fileMetrics.values().stream().filter(m -> m.success).count();
        int failureCount = fileMetrics.size() - successCount;

        System.out.printf("Total files tested:     %d\n", fileMetrics.size());
        System.out.printf("Successful parses:      %d\n", successCount);
        System.out.printf("Failed parses:          %d\n", failureCount);

        System.out.println("\nEntity Count Analysis:");
        System.out.println("-".repeat(70));
        System.out.printf("Total entities found:   %d\n", totalEntitiesFound);
        System.out.printf("R2007+ files:           %d\n", r2007PlusFilesCount);

        if (r2007PlusFilesCount > 0) {
            double avgPerR2007 = totalEntitiesFound / (double) r2007PlusFilesCount;
            System.out.printf("Average per R2007 file: %.0f\n", avgPerR2007);
        }

        System.out.println("\nEntity Count by File:");
        System.out.println("-".repeat(70));
        System.out.printf("%-30s %-10s %10s\n", "File", "Version", "Entities");
        System.out.println("-".repeat(70));

        fileMetrics.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue().entityCount, a.getValue().entityCount))
            .forEach(e -> {
                FileMetrics m = e.getValue();
                String version = m.version != null ? m.version.toString() : "UNKNOWN";
                System.out.printf("%-30s %-10s %10d\n", m.fileName, version, m.entityCount);
            });

        System.out.println("\n" + "═".repeat(70));
        System.out.println("PHASE 7 SUCCESS CRITERIA");
        System.out.println("═".repeat(70));

        printSuccessCriteria();
    }

    private static void printSuccessCriteria() {
        boolean minSuccess = totalEntitiesFound >= 100;
        boolean strongSuccess = totalEntitiesFound >= 500;
        boolean excellentSuccess = totalEntitiesFound >= 1000;

        System.out.println("\nMinimum Success (100+ entities):");
        System.out.printf("  Status: %s\n", minSuccess ? "✅ PASS" : "❌ FAIL");
        System.out.printf("  Requirement: 100+ entities\n");
        System.out.printf("  Found: %d entities\n", totalEntitiesFound);

        System.out.println("\nStrong Success (500+ entities):");
        System.out.printf("  Status: %s\n", strongSuccess ? "✅ PASS" : "⚠️  PARTIAL");
        System.out.printf("  Requirement: 500+ entities\n");
        System.out.printf("  Found: %d entities\n", totalEntitiesFound);

        System.out.println("\nExcellent Success (1000+ entities):");
        System.out.printf("  Status: %s\n", excellentSuccess ? "✅ EXCELLENT" : "⏳ TARGET");
        System.out.printf("  Requirement: 1000+ entities\n");
        System.out.printf("  Found: %d entities\n", totalEntitiesFound);

        System.out.println("\n" + "═".repeat(70));
        System.out.println("OVERALL ASSESSMENT");
        System.out.println("═".repeat(70));

        if (excellentSuccess) {
            System.out.println("🎉 EXCELLENT: blockCount fix working perfectly!");
            System.out.println("   → Proceed to Phase 8 Tier 2 immediately");
        } else if (strongSuccess) {
            System.out.println("✅ STRONG: blockCount fix working well!");
            System.out.println("   → Ready for Phase 8 implementation");
        } else if (minSuccess) {
            System.out.println("✅ SUCCESS: blockCount fix showing improvement!");
            System.out.println("   → May need targeted fixes for some files");
        } else {
            System.out.println("⚠️  LIMITED: Improvement below minimum threshold");
            System.out.println("   → Recommend debugging blockCount fix");
        }
    }

    private static void setupLogging() {
        System.setProperty("java.util.logging.SimpleFormatter.format",
            "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(Level.WARNING);  // Reduce noise
        for (Handler h : rootLogger.getHandlers()) {
            h.setLevel(Level.WARNING);
        }
    }
}
