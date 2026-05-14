package io.dwg.test;

import io.dwg.core.exception.DwgVersionException;
import io.dwg.core.version.DwgVersion;
import io.dwg.core.version.DwgVersionDetector;

/**
 * DWG 버전 감지 테스트 (JUnit 없이 실행 가능)
 */
public class DwgVersionDetectionTest {

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  DWG 버전 감지 테스트");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        int passed = 0;
        int failed = 0;

        // 각 버전 감지 테스트
        passed += testDetectVersion("R13", "AC1012", DwgVersion.R13) ? 1 : 0;
        failed += testDetectVersion("R13", "AC1012", DwgVersion.R13) ? 0 : 1;

        passed += testDetectVersion("R14", "AC1014", DwgVersion.R14) ? 1 : 0;
        failed += testDetectVersion("R14", "AC1014", DwgVersion.R14) ? 0 : 1;

        passed += testDetectVersion("R2000", "AC1015", DwgVersion.R2000) ? 1 : 0;
        failed += testDetectVersion("R2000", "AC1015", DwgVersion.R2000) ? 0 : 1;

        passed += testDetectVersion("R2004", "AC1018", DwgVersion.R2004) ? 1 : 0;
        failed += testDetectVersion("R2004", "AC1018", DwgVersion.R2004) ? 0 : 1;

        passed += testDetectVersion("R2007", "AC1021", DwgVersion.R2007) ? 1 : 0;
        failed += testDetectVersion("R2007", "AC1021", DwgVersion.R2007) ? 0 : 1;

        passed += testDetectVersion("R2010", "AC1024", DwgVersion.R2010) ? 1 : 0;
        failed += testDetectVersion("R2010", "AC1024", DwgVersion.R2010) ? 0 : 1;

        passed += testDetectVersion("R2013", "AC1027", DwgVersion.R2013) ? 1 : 0;
        failed += testDetectVersion("R2013", "AC1027", DwgVersion.R2013) ? 0 : 1;

        passed += testDetectVersion("R2018", "AC1032", DwgVersion.R2018) ? 1 : 0;
        failed += testDetectVersion("R2018", "AC1032", DwgVersion.R2018) ? 0 : 1;

        // 버전 비교 메서드 테스트
        testVersionComparisons();

        // 결과 출력
        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.printf("  테스트 결과: %d 통과, %d 실패\n", passed, failed);
        System.out.println("═══════════════════════════════════════════════════════════════");
    }

    // ================================================================
    // 버전 감지 테스트
    // ================================================================
    private static boolean testDetectVersion(String name, String versionStr, DwgVersion expected) {
        try {
            byte[] header = createDwgHeader(versionStr);
            DwgVersion detected = DwgVersionDetector.detect(header);

            if (detected == expected && detected.versionString().equals(versionStr)) {
                System.out.printf("  ✓ %s 감지 성공 (%s)\n", name, versionStr);
                return true;
            } else {
                System.out.printf("  ✗ %s 감지 실패: 기대=%s, 실제=%s\n", name, expected, detected);
                return false;
            }
        } catch (Exception e) {
            System.out.printf("  ✗ %s 감지 예외: %s\n", name, e.getMessage());
            return false;
        }
    }

    // ================================================================
    // 버전 비교 메서드 테스트
    // ================================================================
    private static void testVersionComparisons() {
        System.out.println("\n─────────────────────────────────────────────────────────────");
        System.out.println("버전 비교 메서드 테스트");
        System.out.println("─────────────────────────────────────────────────────────────");

        // until() 테스트
        if (DwgVersion.R2000.until(DwgVersion.R2004)) {
            System.out.println("  ✓ R2000.until(R2004) = true");
        }
        if (DwgVersion.R2004.until(DwgVersion.R2004)) {
            System.out.println("  ✓ R2004.until(R2004) = true");
        }
        if (!DwgVersion.R2007.until(DwgVersion.R2004)) {
            System.out.println("  ✓ R2007.until(R2004) = false");
        }

        // from() 테스트
        if (DwgVersion.R2007.from(DwgVersion.R2004)) {
            System.out.println("  ✓ R2007.from(R2004) = true");
        }
        if (DwgVersion.R2004.from(DwgVersion.R2004)) {
            System.out.println("  ✓ R2004.from(R2004) = true");
        }
        if (!DwgVersion.R2000.from(DwgVersion.R2004)) {
            System.out.println("  ✓ R2000.from(R2004) = false");
        }

        // between() 테스트
        if (DwgVersion.R2004.between(DwgVersion.R2000, DwgVersion.R2007)) {
            System.out.println("  ✓ R2004.between(R2000, R2007) = true");
        }
        if (!DwgVersion.R13.between(DwgVersion.R2000, DwgVersion.R2007)) {
            System.out.println("  ✓ R13.between(R2000, R2007) = false");
        }

        // 특수 비교 메서드
        if (DwgVersion.R2007.isR2007OrLater()) {
            System.out.println("  ✓ R2007.isR2007OrLater() = true");
        }
        if (!DwgVersion.R2004.isR2007OrLater()) {
            System.out.println("  ✓ R2004.isR2007OrLater() = false");
        }
        if (DwgVersion.R2007.usesUnicode()) {
            System.out.println("  ✓ R2007.usesUnicode() = true");
        }
        if (!DwgVersion.R2004.usesUnicode()) {
            System.out.println("  ✓ R2004.usesUnicode() = false");
        }
    }

    // ================================================================
    // 헬퍼 메서드
    // ================================================================
    private static byte[] createDwgHeader(String versionString) {
        byte[] header = new byte[6];
        byte[] versionBytes = versionString.getBytes();
        System.arraycopy(versionBytes, 0, header, 0, versionBytes.length);
        return header;
    }
}
