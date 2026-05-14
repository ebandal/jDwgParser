package test.java.phase9;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.core.version.DwgVersion;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Phase 9: R2004 Diagnostic - Identify parsing failures
 *
 * 목표:
 * - R2004 파일 파싱 실패 원인 분석
 * - 에러 메시지 수집 및 패턴 분석
 * - 다음 단계 최적화 방향 결정
 */
public class Phase9R2004Diagnostic {

    private static class FileResult {
        String fileName;
        DwgVersion version;
        int entityCount;
        boolean success;
        String errorMessage;
        String errorType;
    }

    private static Map<String, FileResult> results = new HashMap<>();

    public static void main(String[] args) {
        setupLogging();

        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║      PHASE 9: R2004 DIAGNOSTIC TEST                        ║");
        System.out.println("║      Target: Identify R2004 parsing failures              ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");

        testR2004Files();

        System.out.println("\n" + "═".repeat(70));
        System.out.println("R2004 DIAGNOSTIC RESULTS");
        System.out.println("═".repeat(70));

        printResults();
    }

    private static void testR2004Files() {
        String dwgPath = "DWG";
        File dwgDir = new File(dwgPath);

        if (!dwgDir.exists()) {
            System.out.println("⚠️  DWG directory not found. Trying alternatives...");
            String[] altPaths = {"dwg", "./DWG", "./dwg", "../DWG", "samples"};
            for (String alt : altPaths) {
                File altDir = new File(alt);
                if (altDir.exists()) {
                    dwgPath = alt;
                    dwgDir = altDir;
                    break;
                }
            }
            if (!dwgDir.exists()) {
                System.out.println("Could not find DWG directory");
                return;
            }
        }

        System.out.println("Scanning: " + dwgDir.getAbsolutePath());
        System.out.println("Filter: R2004 files only\n");

        try (Stream<Path> paths = Files.walk(Paths.get(dwgPath))) {
            paths.filter(p -> (p.toString().endsWith(".dwg") || p.toString().endsWith(".DWG")))
                 .filter(p -> {
                     try {
                         DwgReader reader = DwgReader.defaultReader();
                         DwgVersion v = reader.detectVersion(p);
                         return v == DwgVersion.R2004;
                     } catch (Exception e) {
                         return false;
                     }
                 })
                 .sorted()
                 .forEach(Phase9R2004Diagnostic::testFile);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void testFile(Path filePath) {
        String fileName = filePath.getFileName().toString();
        FileResult result = new FileResult();
        result.fileName = fileName;

        System.out.printf("Testing R2004: %-30s ", fileName);

        try {
            DwgReader reader = DwgReader.defaultReader();
            DwgVersion version = reader.detectVersion(filePath);
            result.version = version;

            System.out.printf("[%s] ", version);

            try {
                DwgDocument doc = reader.open(filePath);
                var objects = doc.objectMap();

                if (objects == null) {
                    result.entityCount = 0;
                    result.success = false;
                    result.errorType = "NO_OBJECTS";
                    result.errorMessage = "objectMap() returned null";
                    System.out.println("⚠️  No objects (null)");
                } else {
                    result.entityCount = objects.size();
                    result.success = true;
                    System.out.printf("✅ %d entities\n", objects.size());
                }
            } catch (Exception parseEx) {
                result.success = false;
                result.errorType = parseEx.getClass().getSimpleName();
                result.errorMessage = parseEx.getMessage();
                System.out.printf("❌ %s: %s\n", result.errorType,
                    (result.errorMessage != null ? result.errorMessage.substring(0, Math.min(50, result.errorMessage.length())) : "unknown"));
            }

        } catch (Exception e) {
            result.success = false;
            result.errorType = e.getClass().getSimpleName();
            result.errorMessage = e.getMessage();
            System.out.printf("❌ %s\n", result.errorType);
        }

        results.put(fileName, result);
    }

    private static void printResults() {
        System.out.println("\nR2004 File Summary:");
        System.out.println("-".repeat(70));

        int success = (int) results.values().stream().filter(r -> r.success).count();
        int failure = results.size() - success;

        System.out.printf("Total R2004 files: %d\n", results.size());
        System.out.printf("Successful:       %d\n", success);
        System.out.printf("Failed:           %d\n", failure);

        if (failure > 0) {
            System.out.println("\nFailure Analysis:");
            System.out.println("-".repeat(70));

            Map<String, Long> errorCounts = new HashMap<>();
            for (FileResult r : results.values()) {
                if (!r.success) {
                    String errorType = r.errorType != null ? r.errorType : "UNKNOWN";
                    errorCounts.put(errorType, errorCounts.getOrDefault(errorType, 0L) + 1);
                }
            }

            errorCounts.forEach((error, count) -> {
                System.out.printf("%-30s %d files\n", error + ":", count);
            });

            System.out.println("\nDetailed Errors:");
            System.out.println("-".repeat(70));
            results.values().stream()
                .filter(r -> !r.success)
                .forEach(r -> {
                    System.out.printf("%-30s %s\n", r.fileName, r.errorType);
                    if (r.errorMessage != null) {
                        System.out.printf("  -> %s\n", r.errorMessage.substring(0,
                            Math.min(65, r.errorMessage.length())));
                    }
                });
        }

        System.out.println("\n" + "═".repeat(70));
        System.out.println("NEXT STEPS");
        System.out.println("═".repeat(70));

        if (success == 0) {
            System.out.println("\n⚠️  All R2004 files failed to parse");
            System.out.println("   Recommendation: Debug header decryption and section reading");
        } else if (success < results.size()) {
            System.out.println("\n✅ Some R2004 files parse successfully");
            System.out.println("   Next: Analyze specific failures for patterns");
        } else {
            System.out.println("\n🎉 All R2004 files parse successfully!");
            System.out.println("   Ready to move to R2007+ optimization");
        }
    }

    private static void setupLogging() {
        System.setProperty("java.util.logging.SimpleFormatter.format",
            "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(Level.WARNING);
        for (var h : rootLogger.getHandlers()) {
            h.setLevel(Level.WARNING);
        }
    }
}
