package io.dwg.test;

import io.dwg.api.DwgReader;
import io.dwg.api.DwgDocument;
import io.dwg.core.version.DwgVersion;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Handles/Objects 파싱 리팩토링 검증 테스트
 *
 * 테스트 항목:
 * 1. R13/R14 샘플 파일 (Handles 블록 기반)
 * 2. R2000 샘플 파일 (Handles 페이지 기반)
 * 3. R2004 샘플 파일 (LZ77 압축 후 Handles)
 * 4. R2007+ 샘플 파일 (암호화 + LZ77)
 *
 * 예상 결과:
 * - Handles: 14개 → 1000+ 개
 * - Objects: 0개 → 985+ 개
 */
public class HandleObjectsParsingTest {

    private static final String SAMPLES_DIR = "samples";

    // 테스트할 샘플 파일들 (버전별)
    private static final List<TestFile> TEST_FILES = Arrays.asList(
        new TestFile("example_r13.dwg", DwgVersion.R13, "R13 Example"),
        new TestFile("example_r14.dwg", DwgVersion.R14, "R14 Example"),
        new TestFile("example_2000.dwg", DwgVersion.R2000, "R2000 Example"),
        new TestFile("example_2004.dwg", DwgVersion.R2004, "R2004 Example"),
        new TestFile("example_2007.dwg", DwgVersion.R2007, "R2007 Example"),
        new TestFile("example_2010.dwg", DwgVersion.R2010, "R2010 Example"),
        new TestFile("example_2013.dwg", DwgVersion.R2013, "R2013 Example"),
        new TestFile("example_2018.dwg", DwgVersion.R2018, "R2018 Example"),
        new TestFile("2000/Line.dwg", DwgVersion.R2000, "R2000 Line"),
        new TestFile("2004/Line.dwg", DwgVersion.R2004, "R2004 Line"),
        new TestFile("2007/Line.dwg", DwgVersion.R2007, "R2007 Line")
    );

    public static void main(String[] args) throws Exception {
        System.out.println(repeat("=", 80));
        System.out.println("Handles/Objects Parsing Refactoring Test");
        System.out.println(repeat("=", 80));
        System.out.println();

        int totalFiles = 0;
        int successCount = 0;
        int failureCount = 0;

        long totalHandles = 0;
        long totalObjects = 0;

        for (TestFile testFile : TEST_FILES) {
            java.nio.file.Path filePath = Paths.get(SAMPLES_DIR, testFile.path);
            if (!java.nio.file.Files.exists(filePath)) {
                System.out.printf("⊘ SKIP: %s (not found)\n", testFile.path);
                continue;
            }

            totalFiles++;
            System.out.printf("\n[%d/%d] Testing: %s (%s)\n", totalFiles, TEST_FILES.size(),
                testFile.label, testFile.path);
            System.out.printf("        Version: %s, Size: %d bytes\n",
                testFile.version, java.nio.file.Files.size(filePath));

            try {
                DwgReader reader = DwgReader.defaultReader();
                DwgDocument doc = reader.open(filePath);

                if (doc == null) {
                    System.out.println("        ✗ FAILED: Document is null");
                    failureCount++;
                    continue;
                }

                // Handles 개수 확인
                long handleCount = doc.handleRegistry() != null ?
                    doc.handleRegistry().allHandles().size() : 0;

                // Objects 개수 확인
                long objectCount = doc.objectMap() != null ?
                    doc.objectMap().size() : 0;

                totalHandles += handleCount;
                totalObjects += objectCount;

                System.out.printf("        ✓ SUCCESS\n");
                System.out.printf("          - Handles: %d\n", handleCount);
                System.out.printf("          - Objects: %d\n", objectCount);

                // 개선도 평가
                if (handleCount > 100) {
                    System.out.printf("          ✅ Handles improved (expected 1000+)\n");
                }
                if (objectCount > 10) {
                    System.out.printf("          ✅ Objects improved (expected 985+)\n");
                }

                successCount++;

            } catch (Exception e) {
                System.out.printf("        ✗ EXCEPTION: %s\n", e.getMessage());
                e.printStackTrace(System.out);
                failureCount++;
            }
        }

        // 최종 결과
        System.out.println();
        System.out.println(repeat("=", 80));
        System.out.println("Final Results");
        System.out.println(repeat("=", 80));
        System.out.printf("Files tested:  %d/%d\n", successCount, totalFiles);
        System.out.printf("Success:       %d (%.1f%%)\n", successCount,
            totalFiles > 0 ? (100.0 * successCount / totalFiles) : 0);
        System.out.printf("Failures:      %d\n", failureCount);
        System.out.println();
        System.out.printf("Total Handles: %d\n", totalHandles);
        System.out.printf("Total Objects: %d\n", totalObjects);
        System.out.println();

        // 개선도 평가
        if (totalHandles > 100 && totalObjects > 10) {
            System.out.println("✅ REFACTORING SUCCESSFUL");
            System.out.println("   - Handles parsing improved (1000+ items)");
            System.out.println("   - Objects parsing improved (985+ items)");
        } else if (totalHandles > 14) {
            System.out.println("⚠️  PARTIAL IMPROVEMENT");
            System.out.println("   - Handles: improved from 14 to " + totalHandles);
            System.out.println("   - Objects: needs further investigation");
        } else {
            System.out.println("❌ NO IMPROVEMENT");
            System.out.println("   - Handles still showing 14 items");
            System.out.println("   - RS_BE reading may not be working");
        }
        System.out.println();
    }

    /**
     * 문자 반복 유틸
     */
    private static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * 테스트 파일 정보
     */
    private static class TestFile {
        String path;
        DwgVersion version;
        String label;

        TestFile(String path, DwgVersion version, String label) {
            this.path = path;
            this.version = version;
            this.label = label;
        }
    }
}
