package run;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Test all DWG sample files in the samples directory for parseability.
 */
public class SampleFileTester {

    static class TestResult {
        String filePath;
        boolean success;
        String version;
        int entityCount;
        int objectCount;
        long fileSize;
        String errorMessage;

        TestResult(String filePath) {
            this.filePath = filePath;
            this.success = false;
            this.version = "Unknown";
            this.entityCount = 0;
            this.objectCount = 0;
            this.fileSize = 0;
            this.errorMessage = "";
        }
    }

    public static void main(String[] args) throws IOException {
        Path samplesDir = Paths.get("samples");

        if (!Files.exists(samplesDir)) {
            System.out.println("❌ samples directory not found");
            return;
        }

        System.out.println("🔍 Scanning for DWG files in samples directory...\n");

        List<TestResult> results = new ArrayList<>();

        // Find all .dwg files recursively
        try (Stream<Path> files = Files.walk(samplesDir)) {
            files.filter(p -> p.toString().toLowerCase().endsWith(".dwg"))
                .forEach(filePath -> {
                    TestResult result = testFile(filePath);
                    results.add(result);
                });
        }

        // Print results
        printResults(results);
    }

    private static TestResult testFile(Path filePath) {
        TestResult result = new TestResult(filePath.toString());

        // Suppress debug output temporarily
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        try {
            System.setOut(new PrintStream(new java.io.OutputStream() {
                @Override public void write(int b) {}
            }));
            System.setErr(new PrintStream(new java.io.OutputStream() {
                @Override public void write(int b) {}
            }));

            // Get file size
            result.fileSize = Files.size(filePath);

            // Try to parse the file
            DwgDocument doc = DwgReader.defaultReader().open(filePath);

            // Extract information
            result.version = doc.version().toString();
            result.objectCount = doc.objectMap().size();
            result.entityCount = doc.entities().size();
            result.success = true;

        } catch (Exception e) {
            result.success = false;
            result.errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            System.setOut(oldOut);
            System.setErr(oldErr);
        }

        return result;
    }

    private static void printResults(List<TestResult> results) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 TEST SUMMARY");
        System.out.println("=".repeat(80));

        // Overall statistics
        long passCount = results.stream().filter(r -> r.success).count();
        long failCount = results.stream().filter(r -> !r.success).count();
        double successRate = results.isEmpty() ? 0 : (passCount * 100.0 / results.size());

        System.out.printf("\nTotal Files: %d\n", results.size());
        System.out.printf("✅ Passed: %d (%.1f%%)\n", passCount, successRate);
        System.out.printf("❌ Failed: %d\n", failCount);

        // Group by version
        System.out.println("\n" + "-".repeat(80));
        System.out.println("Results by Version:");
        System.out.println("-".repeat(80));

        Map<String, List<TestResult>> byVersion = new TreeMap<>();
        for (TestResult r : results) {
            byVersion.computeIfAbsent(r.version, k -> new ArrayList<>()).add(r);
        }

        for (String version : byVersion.keySet()) {
            List<TestResult> versionResults = byVersion.get(version);
            long versionPass = versionResults.stream().filter(r -> r.success).count();
            System.out.printf("\n%s: %d/%d passed\n", version, versionPass, versionResults.size());

            for (TestResult r : versionResults) {
                if (!r.success) {
                    System.out.printf("  ❌ %s - %s\n", r.filePath, r.errorMessage);
                }
            }
        }

        // Failed files detail
        if (failCount > 0) {
            System.out.println("\n" + "-".repeat(80));
            System.out.println("Failed Files Details:");
            System.out.println("-".repeat(80));

            results.stream()
                .filter(r -> !r.success)
                .forEach(r -> {
                    System.out.printf("\nFile: %s\n", r.filePath);
                    System.out.printf("Error: %s\n", r.errorMessage);
                    System.out.printf("Size: %d bytes\n", r.fileSize);
                });
        }

        System.out.println("\n" + "=".repeat(80));
    }
}
