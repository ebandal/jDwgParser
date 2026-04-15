package io.dwg.test;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.api.DwgWriter;
import io.dwg.core.version.DwgVersion;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Round-trip test: Read DWG → Write DWG → Verify structure
 * Usage: java -cp target/classes io.dwg.test.DwgRoundTripTest
 */
public class DwgRoundTripTest {

    // Use R2004 for testing (R2000 parsing needs debugging)
    private static final Path SAMPLE_DIR = Paths.get("samples/2004");
    private static final Path OUTPUT_DIR = Paths.get("target/test-output");

    public static void main(String[] args) throws Exception {
        System.out.println("Starting DWG Round-Trip Tests\n");
        testRoundTripSimpleCircle();
        testRoundTripWithMultipleEntities();
        testWriteEmptyDocument();
        testVersionPreservation();
        testR2007Write();
        testR13Write();
        testR14Write();
        testSampleR13File();
        testSampleR14File();
        System.out.println("\n✓ All round-trip tests completed");
    }

    public static void testRoundTripSimpleCircle() throws Exception {
        Path inputFile = SAMPLE_DIR.resolve("circle.dwg");
        if (!Files.exists(inputFile)) {
            System.out.println("⊘ Skipping: " + inputFile);
            return;
        }

        System.out.println("\n=== Test 1: circle.dwg ===");

        // Read original file
        DwgDocument original = DwgReader.defaultReader().open(inputFile);
        if (original == null) {
            System.out.println("✗ Failed to read original file");
            return;
        }

        System.out.println("Original version: " + original.version());
        int originalEntityCount = original.entities().size();
        System.out.println("Original entity count: " + originalEntityCount);

        // Write to new file
        Files.createDirectories(OUTPUT_DIR);
        Path outputFile = OUTPUT_DIR.resolve("circle-output.dwg");
        try {
            DwgWriter writer = DwgWriter.forVersion(original.version());
            writer.write(original, outputFile);
        } catch (Exception e) {
            System.out.println("✗ Write error: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        if (!Files.exists(outputFile)) {
            System.out.println("✗ Failed to write output file");
            return;
        }

        long outputSize = Files.size(outputFile);
        System.out.println("Output file size: " + outputSize + " bytes");

        // Read the written file back
        DwgDocument rewritten = DwgReader.defaultReader().open(outputFile);
        if (rewritten == null) {
            System.out.println("✗ Failed to read rewritten file");
            return;
        }

        System.out.println("Rewritten version: " + rewritten.version());

        // Verify structure
        if (originalEntityCount == rewritten.entities().size()) {
            System.out.println("✓ Entity count preserved: " + originalEntityCount);
        } else {
            System.out.println("✗ Entity count mismatch: " + originalEntityCount + " vs " + rewritten.entities().size());
        }

        if (original.objectMap().size() == rewritten.objectMap().size()) {
            System.out.println("✓ Object count preserved: " + original.objectMap().size());
        } else {
            System.out.println("✗ Object count mismatch: " + original.objectMap().size() + " vs " + rewritten.objectMap().size());
        }
    }

    public static void testRoundTripWithMultipleEntities() throws Exception {
        Path inputFile = SAMPLE_DIR.resolve("Arc.dwg");
        if (!Files.exists(inputFile)) {
            System.out.println("⊘ Skipping: " + inputFile);
            return;
        }

        System.out.println("\n=== Test 2: Arc.dwg ===");

        DwgDocument original = DwgReader.defaultReader().open(inputFile);
        if (original == null) {
            System.out.println("✗ Failed to read original file");
            return;
        }

        System.out.println("Original entities: " + original.entities().size());

        Files.createDirectories(OUTPUT_DIR);
        Path outputFile = OUTPUT_DIR.resolve("arc-output.dwg");
        DwgWriter writer = DwgWriter.forVersion(original.version());
        writer.write(original, outputFile);

        DwgDocument rewritten = DwgReader.defaultReader().open(outputFile);
        if (rewritten == null) {
            System.out.println("✗ Failed to read rewritten file");
            return;
        }

        if (original.entities().size() == rewritten.entities().size()) {
            System.out.println("✓ Multi-entity round-trip passed");
        } else {
            System.out.println("✗ Entity count mismatch: " + original.entities().size() + " vs " + rewritten.entities().size());
        }
    }

    public static void testWriteEmptyDocument() throws Exception {
        System.out.println("\n=== Test 3: Empty Document ===");

        DwgDocument doc = new DwgDocument(DwgVersion.R2000);

        Files.createDirectories(OUTPUT_DIR);
        Path outputFile = OUTPUT_DIR.resolve("empty-output.dwg");
        DwgWriter writer = DwgWriter.forVersion(DwgVersion.R2000);

        try {
            writer.write(doc, outputFile);
            if (Files.exists(outputFile)) {
                System.out.println("✓ Empty document write test passed");
                System.out.println("  File size: " + Files.size(outputFile) + " bytes");
            } else {
                System.out.println("✗ Failed to write empty document");
            }
        } catch (Exception e) {
            System.out.println("✗ Exception writing empty document: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void testVersionPreservation() throws Exception {
        Path inputFile = SAMPLE_DIR.resolve("circle.dwg");
        if (!Files.exists(inputFile)) {
            System.out.println("⊘ Skipping: " + inputFile);
            return;
        }

        System.out.println("\n=== Test 4: Version Preservation ===");

        DwgDocument original = DwgReader.defaultReader().open(inputFile);
        if (original == null) {
            return;
        }

        DwgVersion originalVersion = original.version();

        Files.createDirectories(OUTPUT_DIR);
        Path outputFile = OUTPUT_DIR.resolve("version-test-output.dwg");

        DwgWriter writer = DwgWriter.forVersion(originalVersion);
        writer.write(original, outputFile);

        DwgDocument rewritten = DwgReader.defaultReader().open(outputFile);
        if (rewritten == null) {
            System.out.println("✗ Failed to read rewritten file");
            return;
        }

        if (originalVersion == rewritten.version()) {
            System.out.println("✓ Version preservation test passed: " + originalVersion);
        } else {
            System.out.println("✗ Version mismatch: " + originalVersion + " vs " + rewritten.version());
        }
    }

    public static void testR2007Write() throws Exception {
        System.out.println("\n=== Test 5: R2007 Write ===");

        // Test writing an empty R2007 document
        DwgDocument doc = new DwgDocument(DwgVersion.R2007);

        Files.createDirectories(OUTPUT_DIR);
        Path outputFile = OUTPUT_DIR.resolve("r2007-empty-output.dwg");

        try {
            DwgWriter writer = DwgWriter.forVersion(DwgVersion.R2007);
            writer.write(doc, outputFile);
            System.out.println("✓ R2007 file write successful");

            long fileSize = Files.size(outputFile);
            System.out.println("  Output file size: " + fileSize + " bytes");

            if (fileSize > 0x480) {
                System.out.println("✓ Output file has valid size (> header size 0x480)");
            } else {
                System.out.println("✗ Output file too small");
            }

            // Verify header signature
            byte[] allBytes = java.nio.file.Files.readAllBytes(outputFile);
            if (allBytes.length >= 6) {
                String version = new String(allBytes, 0, 6, java.nio.charset.StandardCharsets.US_ASCII);
                if (version.equals("AC1021")) {
                    System.out.println("✓ R2007 version string correct");
                } else {
                    System.out.println("✗ Wrong version string: " + version);
                }
            }
        } catch (Exception e) {
            System.out.println("✗ R2007 write error: " + e.getMessage());
            // e.printStackTrace();  // Suppress stack trace for cleaner output
        }
    }

    public static void testR13Write() throws Exception {
        System.out.println("\n=== Test 6: R13 Write ===");

        // Create empty R13 document
        DwgDocument doc = new DwgDocument(DwgVersion.R13);

        Files.createDirectories(OUTPUT_DIR);
        Path outputFile = OUTPUT_DIR.resolve("r13-empty-output.dwg");

        try {
            DwgWriter writer = DwgWriter.forVersion(DwgVersion.R13);
            writer.write(doc, outputFile);
            System.out.println("✓ R13 file write successful");

            long fileSize = Files.size(outputFile);
            System.out.println("  Output file size: " + fileSize + " bytes");

            if (fileSize > 100) {
                System.out.println("✓ Output file has valid size");
            } else {
                System.out.println("✗ Output file too small");
            }

            // Verify header signature
            byte[] allBytes = java.nio.file.Files.readAllBytes(outputFile);
            if (allBytes.length >= 6) {
                String version = new String(allBytes, 0, 6, java.nio.charset.StandardCharsets.US_ASCII);
                if (version.equals("AC1012")) {
                    System.out.println("✓ R13 version string correct");
                } else {
                    System.out.println("✗ Wrong version string: " + version);
                }
            }
        } catch (Exception e) {
            System.out.println("✗ R13 write error: " + e.getMessage());
        }
    }

    public static void testR14Write() throws Exception {
        System.out.println("\n=== Test 7: R14 Write ===");

        // Create empty R14 document
        DwgDocument doc = new DwgDocument(DwgVersion.R14);

        Files.createDirectories(OUTPUT_DIR);
        Path outputFile = OUTPUT_DIR.resolve("r14-empty-output.dwg");

        try {
            DwgWriter writer = DwgWriter.forVersion(DwgVersion.R14);
            writer.write(doc, outputFile);
            System.out.println("✓ R14 file write successful");

            long fileSize = Files.size(outputFile);
            System.out.println("  Output file size: " + fileSize + " bytes");

            if (fileSize > 100) {
                System.out.println("✓ Output file has valid size");
            } else {
                System.out.println("✗ Output file too small");
            }

            // Verify header signature
            byte[] allBytes = java.nio.file.Files.readAllBytes(outputFile);
            if (allBytes.length >= 6) {
                String version = new String(allBytes, 0, 6, java.nio.charset.StandardCharsets.US_ASCII);
                if (version.equals("AC1014")) {
                    System.out.println("✓ R14 version string correct");
                } else {
                    System.out.println("✗ Wrong version string: " + version);
                }
            }
        } catch (Exception e) {
            System.out.println("✗ R14 write error: " + e.getMessage());
        }
    }

    public static void testSampleR13File() throws Exception {
        Path inputFile = Paths.get("samples/example_r13.dwg");
        if (!Files.exists(inputFile)) {
            System.out.println("\n=== Test 8: Sample R13 File ===");
            System.out.println("⊘ Skipping: " + inputFile);
            return;
        }

        System.out.println("\n=== Test 8: Sample R13 File ===");

        try {
            DwgDocument doc = DwgReader.defaultReader().open(inputFile);
            if (doc == null) {
                System.out.println("✗ Failed to read R13 sample file");
                return;
            }

            System.out.println("✓ Successfully read sample R13 file header");
            System.out.println("  Version: " + doc.version());
            System.out.println("  Entity count: " + doc.entities().size());
            System.out.println("  Object count: " + doc.objectMap().size());

        } catch (Exception e) {
            System.out.println("✗ Error reading R13 sample: " + e.getClass().getSimpleName());
        }
    }

    public static void testSampleR14File() throws Exception {
        Path inputFile = Paths.get("samples/example_r14.dwg");
        if (!Files.exists(inputFile)) {
            System.out.println("\n=== Test 9: Sample R14 File ===");
            System.out.println("⊘ Skipping: " + inputFile);
            return;
        }

        System.out.println("\n=== Test 9: Sample R14 File ===");

        try {
            DwgDocument doc = DwgReader.defaultReader().open(inputFile);
            if (doc == null) {
                System.out.println("✗ Failed to read R14 sample file");
                return;
            }

            System.out.println("✓ Successfully read sample R14 file header");
            System.out.println("  Version: " + doc.version());
            System.out.println("  Entity count: " + doc.entities().size());
            System.out.println("  Object count: " + doc.objectMap().size());

        } catch (Exception e) {
            System.out.println("✗ Error reading R14 sample: " + e.getClass().getSimpleName());
        }
    }
}
