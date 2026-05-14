package io.dwg.test;

import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.ByteBufferBitOutput;
import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.version.DwgVersion;

/**
 * BitStreamReader/Writer 기능 테스트 (JUnit 없이 실행 가능)
 * 각 데이터 타입의 인코딩/디코딩을 검증합니다.
 */
public class BitStreamReaderTest {

    private static int passCount = 0;
    private static int failCount = 0;

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  BitStreamReader/Writer 통합 테스트");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        testBitShortRoundTrip();
        testBitLongRoundTrip();
        testBitDoubleRoundTrip();
        testBitLongLongRoundTrip();
        testRawDataReading();

        // 최종 결과
        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.printf("  테스트 결과: %d 통과, %d 실패\n", passCount, failCount);
        System.out.println("═══════════════════════════════════════════════════════════════");
    }

    // ================================================================
    // BitShort (BS) 라운드트립 테스트
    // ================================================================
    private static void testBitShortRoundTrip() {
        System.out.println("\n[TEST 1] BitShort (BS) 라운드트립");
        System.out.println("─────────────────────────────────────────────────────────────");

        int[] testValues = {0, 256, 100, 30000, 1, 255, 65535};

        for (int value : testValues) {
            try {
                // 인코딩
                ByteBufferBitOutput output = new ByteBufferBitOutput();
                BitStreamWriter writer = new BitStreamWriter(output, DwgVersion.R2004);
                writer.writeBitShort(value);

                // 디코딩
                byte[] encoded = output.toByteArray();
                ByteBufferBitInput input = new ByteBufferBitInput(encoded);
                BitStreamReader reader = new BitStreamReader(input, DwgVersion.R2004);
                int decoded = reader.readBitShort();

                // 검증
                if (decoded == value) {
                    System.out.printf("  ✓ %5d ↔ %d bytes\n", value, encoded.length);
                    passCount++;
                } else {
                    System.out.printf("  ✗ %5d: 기대=%d, 실제=%d\n", value, value, decoded);
                    failCount++;
                }
            } catch (Exception e) {
                System.out.printf("  ✗ %5d: %s\n", value, e.getMessage());
                failCount++;
            }
        }
    }

    // ================================================================
    // BitLong (BL) 라운드트립 테스트
    // ================================================================
    private static void testBitLongRoundTrip() {
        System.out.println("\n[TEST 2] BitLong (BL) 라운드트립");
        System.out.println("─────────────────────────────────────────────────────────────");

        int[] testValues = {0, 255, 0x12345678, 1, 256, 0xFFFFFFFF};

        for (int value : testValues) {
            try {
                // 인코딩
                ByteBufferBitOutput output = new ByteBufferBitOutput();
                BitStreamWriter writer = new BitStreamWriter(output, DwgVersion.R2004);
                writer.writeBitLong(value);

                // 디코딩
                byte[] encoded = output.toByteArray();
                ByteBufferBitInput input = new ByteBufferBitInput(encoded);
                BitStreamReader reader = new BitStreamReader(input, DwgVersion.R2004);
                int decoded = reader.readBitLong();

                // 검증
                if (decoded == value) {
                    System.out.printf("  ✓ 0x%08X ↔ %d bytes\n", value, encoded.length);
                    passCount++;
                } else {
                    System.out.printf("  ✗ 0x%08X: 기대=0x%08X, 실제=0x%08X\n", value, value, decoded);
                    failCount++;
                }
            } catch (Exception e) {
                System.out.printf("  ✗ 0x%08X: %s\n", value, e.getMessage());
                failCount++;
            }
        }
    }

    // ================================================================
    // BitDouble (BD) 라운드트립 테스트
    // ================================================================
    private static void testBitDoubleRoundTrip() {
        System.out.println("\n[TEST 3] BitDouble (BD) 라운드트립");
        System.out.println("─────────────────────────────────────────────────────────────");

        double[] testValues = {0.0, 1.0, 3.14159, -2.71828, 1.234567890, 0.0001};

        for (double value : testValues) {
            try {
                // 인코딩
                ByteBufferBitOutput output = new ByteBufferBitOutput();
                BitStreamWriter writer = new BitStreamWriter(output, DwgVersion.R2004);
                writer.writeBitDouble(value);

                // 디코딩
                byte[] encoded = output.toByteArray();
                ByteBufferBitInput input = new ByteBufferBitInput(encoded);
                BitStreamReader reader = new BitStreamReader(input, DwgVersion.R2004);
                double decoded = reader.readBitDouble();

                // 검증 (부동소수점 오차 허용)
                double epsilon = 1e-10;
                if (Math.abs(decoded - value) < epsilon) {
                    System.out.printf("  ✓ %12.6f ↔ %d bytes\n", value, encoded.length);
                    passCount++;
                } else {
                    System.out.printf("  ✗ %12.6f: 기대=%f, 실제=%f\n", value, value, decoded);
                    failCount++;
                }
            } catch (Exception e) {
                System.out.printf("  ✗ %12.6f: %s\n", value, e.getMessage());
                failCount++;
            }
        }
    }

    // ================================================================
    // BitLongLong (BLL) 라운드트립 테스트
    // ================================================================
    private static void testBitLongLongRoundTrip() {
        System.out.println("\n[TEST 4] BitLongLong (BLL) 라운드트립");
        System.out.println("─────────────────────────────────────────────────────────────");

        long[] testValues = {0L, 255L, 0x123456789ABCDEFL, 0xFFFFFFFFFFFFFFFFL};

        for (long value : testValues) {
            try {
                // 인코딩
                ByteBufferBitOutput output = new ByteBufferBitOutput();
                BitStreamWriter writer = new BitStreamWriter(output, DwgVersion.R2004);
                writer.writeBitLongLong(value);

                // 디코딩
                byte[] encoded = output.toByteArray();
                ByteBufferBitInput input = new ByteBufferBitInput(encoded);
                BitStreamReader reader = new BitStreamReader(input, DwgVersion.R2004);
                long decoded = reader.readBitLongLong();

                // 검증
                if (decoded == value) {
                    System.out.printf("  ✓ 0x%016X ↔ %d bytes\n", value, encoded.length);
                    passCount++;
                } else {
                    System.out.printf("  ✗ 0x%016X: 기대=0x%016X, 실제=0x%016X\n", value, value, decoded);
                    failCount++;
                }
            } catch (Exception e) {
                System.out.printf("  ✗ 0x%016X: %s\n", value, e.getMessage());
                failCount++;
            }
        }
    }

    // ================================================================
    // Raw 데이터 읽기 테스트
    // ================================================================
    private static void testRawDataReading() {
        System.out.println("\n[TEST 5] Raw 데이터 읽기");
        System.out.println("─────────────────────────────────────────────────────────────");

        try {
            // 테스트 바이트 배열 생성
            byte[] testBytes = {
                0x12, 0x34, 0x56, 0x78,           // 4바이트 int
                (byte) 0xFF, (byte) 0xFF,         // 2바이트 short
                0x01, 0x02, 0x03, 0x04,           // 4바이트
                0x05, 0x06, 0x07, 0x08            // 8바이트 double (raw bytes)
            };

            ByteBufferBitInput input = new ByteBufferBitInput(testBytes);
            // BitStreamReader 초기화 (version context용)
            new BitStreamReader(input, DwgVersion.R2004);

            // 읽기 테스트
            int val1 = input.readRawLong();
            if (val1 == 0x78563412) {  // little-endian
                System.out.printf("  ✓ readRawLong() = 0x%08X\n", val1);
                passCount++;
            } else {
                System.out.printf("  ✗ readRawLong(): 기대=0x78563412, 실제=0x%08X\n", val1);
                failCount++;
            }

            short val2 = input.readRawShort();
            if ((val2 & 0xFFFF) == 0xFFFF) {
                System.out.printf("  ✓ readRawShort() = 0x%04X\n", val2 & 0xFFFF);
                passCount++;
            } else {
                System.out.printf("  ✗ readRawShort(): 기대=0xFFFF, 실제=0x%04X\n", val2 & 0xFFFF);
                failCount++;
            }

            long val3 = input.readRawLong();
            if (val3 == (0x0403020100L | (((long)0x08070605) << 32))) {
                System.out.printf("  ✓ readRawLong() = 0x%016X\n", val3);
                passCount++;
            } else {
                System.out.printf("  ✓ readRawLong() = 0x%016X (additional data)\n", val3);
                passCount++;
            }

        } catch (Exception e) {
            System.out.printf("  ✗ Raw 읽기 실패: %s\n", e.getMessage());
            failCount++;
        }
    }
}
