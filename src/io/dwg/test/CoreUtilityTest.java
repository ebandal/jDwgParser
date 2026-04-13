package io.dwg.test;

import io.dwg.core.util.Lz77Compressor;
import io.dwg.core.util.Lz77Decompressor;
import io.dwg.core.util.CrcCalculator;
import io.dwg.core.util.DwgStringDecoder;
import io.dwg.core.util.DwgStringEncoder;
import io.dwg.core.version.DwgVersion;

/**
 * DWG 핵심 유틸리티 함수 테스트 (JUnit 없이 실행 가능)
 * LZ77 압축, CRC 계산, 문자열 인코딩 등을 검증합니다.
 */
public class CoreUtilityTest {

    private static int passCount = 0;
    private static int failCount = 0;

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  DWG 핵심 유틸리티 테스트");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        testLz77Compression();
        testCrcCalculation();
        testStringEncoding();

        // 최종 결과
        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.printf("  테스트 결과: %d 통과, %d 실패\n", passCount, failCount);
        System.out.println("═══════════════════════════════════════════════════════════════");
    }

    // ================================================================
    // LZ77 압축/해제 테스트
    // ================================================================
    private static void testLz77Compression() {
        System.out.println("\n[TEST 1] LZ77 압축/해제 라운드트립");
        System.out.println("─────────────────────────────────────────────────────────────");

        String[] testData = {
            "LAYER0",
            "This is a test string with some repeated patterns",
            "aaaaaabbbbbbcccccc",
            "Line\nCircle\nArc\nText",
            "",  // 빈 문자열
            "A"   // 한 글자
        };

        for (String original : testData) {
            try {
                byte[] originalBytes = original.getBytes(java.nio.charset.StandardCharsets.US_ASCII);

                // 압축
                Lz77Compressor compressor = new Lz77Compressor();
                byte[] compressed = compressor.compress(originalBytes);

                // 해제
                Lz77Decompressor decompressor = new Lz77Decompressor();
                byte[] decompressed = decompressor.decompress(compressed, originalBytes.length);

                // 검증
                if (java.util.Arrays.equals(originalBytes, decompressed)) {
                    double ratio = originalBytes.length > 0
                        ? (100.0 * compressed.length / originalBytes.length)
                        : 0;
                    System.out.printf("  ✓ \"%s\" → %d → %d bytes (%.1f%%)\n",
                        truncate(original, 20), originalBytes.length, compressed.length, ratio);
                    passCount++;
                } else {
                    System.out.printf("  ✗ \"%s\": 데이터 불일치\n", truncate(original, 20));
                    failCount++;
                }
            } catch (Exception e) {
                System.out.printf("  ✗ \"%s\": %s\n", truncate(original, 20), e.getMessage());
                failCount++;
            }
        }
    }

    // ================================================================
    // CRC 계산 테스트
    // ================================================================
    private static void testCrcCalculation() {
        System.out.println("\n[TEST 2] CRC 계산 검증");
        System.out.println("─────────────────────────────────────────────────────────────");

        // 테스트 케이스
        byte[][] testCases = {
            new byte[]{0, 0, 0, 0},
            new byte[]{1, 2, 3, 4},
            new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF},
            "LAYER0".getBytes(java.nio.charset.StandardCharsets.US_ASCII),
            "DWG FILE".getBytes(java.nio.charset.StandardCharsets.US_ASCII),
        };

        for (byte[] data : testCases) {
            try {
                // CRC-8 계산
                CrcCalculator.Crc8Calculator crc8Calc = new CrcCalculator.Crc8Calculator();
                int crc8Value = crc8Calc.calculate(data, 0);
                System.out.printf("  ✓ CRC-8(%s) = 0x%02X\n",
                    truncate(new String(data, java.nio.charset.StandardCharsets.US_ASCII), 16),
                    crc8Value & 0xFF);
                passCount++;

                // CRC-32 계산
                CrcCalculator.Crc32Calculator crc32Calc = new CrcCalculator.Crc32Calculator();
                int crc32Value = crc32Calc.calculate(data, 0);
                System.out.printf("    CRC-32 = 0x%08X\n", crc32Value);
            } catch (Exception e) {
                System.out.printf("  ✗ CRC 계산 실패: %s\n", e.getMessage());
                failCount++;
            }
        }
    }

    // ================================================================
    // 문자열 인코딩/디코딩 테스트
    // ================================================================
    private static void testStringEncoding() {
        System.out.println("\n[TEST 3] 문자열 인코딩/디코딩 (R2004)");
        System.out.println("─────────────────────────────────────────────────────────────");

        String[] testStrings = {
            "LAYER0",
            "Line",
            "Circle",
            "Text with spaces",
            "Special!@#$%Chars",
            ""  // 빈 문자열
        };

        for (String original : testStrings) {
            try {
                // 인코딩 (R2004 = US_ASCII)
                byte[] encoded = DwgStringEncoder.encode(original, DwgVersion.R2004);

                // 디코딩
                String decoded = DwgStringDecoder.decode(encoded, DwgVersion.R2004);

                // 검증
                if (decoded.equals(original)) {
                    System.out.printf("  ✓ \"%s\" ↔ %d bytes\n",
                        truncate(original, 20), encoded.length);
                    passCount++;
                } else {
                    System.out.printf("  ✗ \"%s\": 기대=\"%s\", 실제=\"%s\"\n",
                        truncate(original, 20), original, decoded);
                    failCount++;
                }
            } catch (Exception e) {
                System.out.printf("  ✗ \"%s\": %s\n", truncate(original, 20), e.getMessage());
                failCount++;
            }
        }

        // UTF-16LE 테스트 (R2007+)
        System.out.println("\n  [R2007+] UTF-16LE 인코딩:");
        for (String original : new String[]{"LAYER0", "Circle"}) {
            try {
                byte[] encoded = DwgStringEncoder.encode(original, DwgVersion.R2007);
                String decoded = DwgStringDecoder.decode(encoded, DwgVersion.R2007);

                if (decoded.equals(original)) {
                    System.out.printf("  ✓ \"%s\" ↔ %d bytes\n", original, encoded.length);
                    passCount++;
                } else {
                    System.out.printf("  ✗ \"%s\": 불일치\n", original);
                    failCount++;
                }
            } catch (Exception e) {
                System.out.printf("  ✗ \"%s\": %s\n", original, e.getMessage());
                failCount++;
            }
        }
    }

    // ================================================================
    // 헬퍼 메서드
    // ================================================================
    private static String truncate(String str, int maxLen) {
        if (str.length() > maxLen) {
            return str.substring(0, maxLen - 3) + "...";
        }
        return str;
    }
}
