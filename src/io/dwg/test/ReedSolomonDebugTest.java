package io.dwg.test;

import io.dwg.core.util.ReedSolomonDecoder;

/**
 * Reed-Solomon 디코더 디버깅 테스트
 * 기본 동작을 확인하고 버그를 찾습니다.
 */
public class ReedSolomonDebugTest {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Reed-Solomon Decoder Debug Test");
        System.out.println("========================================\n");

        // Test 1: 완전히 깨끗한 데이터 (에러 0개)
        testCleanData();

        // Test 2: 작은 에러 1개
        testOneError();

        // Test 3: 큰 에러 여러 개
        testMultipleErrors();
    }

    private static void testCleanData() {
        System.out.println("[TEST 1] Clean Data (No Errors)");
        System.out.println("================================");

        // 255바이트 깨끗한 데이터
        byte[] block = new byte[255];
        for (int i = 0; i < 255; i++) {
            block[i] = (byte) i;
        }

        System.out.printf("Input: %d bytes of clean sequential data\n", block.length);

        // 에러 감지 (수정 없음)
        int checkErr = ReedSolomonDecoder.decodeBlock(block, false);
        System.out.printf("Check result: %d (0=no error, -1=error detected)\n", checkErr);

        if (checkErr == 0) {
            System.out.println("✓ PASS: Correctly identified as clean data");
        } else {
            System.out.println("✗ FAIL: Incorrectly detected errors in clean data!");
        }
        System.out.println();
    }

    private static void testOneError() {
        System.out.println("[TEST 2] One Error");
        System.out.println("==================");

        byte[] block = new byte[255];
        for (int i = 0; i < 255; i++) {
            block[i] = (byte) i;
        }

        // 한 위치에 에러 주입 (위치 100, 값을 0xFF로 변경)
        int errorPos = 100;
        byte originalValue = block[errorPos];
        block[errorPos] = (byte) 0xFF;

        System.out.printf("Injected error at position %d: 0x%02X -> 0x%02X\n",
            errorPos, originalValue & 0xFF, 0xFF);

        // 에러 감지
        int checkErr = ReedSolomonDecoder.decodeBlock(block, false);
        System.out.printf("Check result: %d (should be -1 for error detected)\n", checkErr);

        // 에러 수정
        int fixErr = ReedSolomonDecoder.decodeBlock(block, true);
        System.out.printf("Fix result: %d (should be 1 for 1 error fixed)\n", fixErr);

        // 복구된 값 확인
        if (fixErr >= 0 && block[errorPos] == originalValue) {
            System.out.println("✓ PASS: Error corrected successfully");
        } else {
            System.out.printf("✗ FAIL: Error not corrected (position %d: expected 0x%02X, got 0x%02X)\n",
                errorPos, originalValue & 0xFF, block[errorPos] & 0xFF);
        }
        System.out.println();
    }

    private static void testMultipleErrors() {
        System.out.println("[TEST 3] Multiple Errors (up to 8)");
        System.out.println("===================================");

        byte[] block = new byte[255];
        for (int i = 0; i < 255; i++) {
            block[i] = (byte) i;
        }

        // 3개 에러 주입
        int[] errorPositions = {50, 100, 200};
        byte[] originalValues = new byte[3];

        for (int i = 0; i < errorPositions.length; i++) {
            originalValues[i] = block[errorPositions[i]];
            block[errorPositions[i]] = (byte) ((block[errorPositions[i]] + 0x55) & 0xFF);
            System.out.printf("Injected error at position %d: 0x%02X -> 0x%02X\n",
                errorPositions[i], originalValues[i] & 0xFF, block[errorPositions[i]] & 0xFF);
        }

        // 에러 수정
        int fixErr = ReedSolomonDecoder.decodeBlock(block, true);
        System.out.printf("Fix result: %d (should be %d for %d errors fixed)\n",
            fixErr, errorPositions.length, errorPositions.length);

        // 복구된 값 확인
        boolean allCorrected = true;
        for (int i = 0; i < errorPositions.length; i++) {
            if (block[errorPositions[i]] != originalValues[i]) {
                allCorrected = false;
                System.out.printf("✗ Position %d: expected 0x%02X, got 0x%02X\n",
                    errorPositions[i], originalValues[i] & 0xFF, block[errorPositions[i]] & 0xFF);
            }
        }

        if (allCorrected && fixErr >= 0) {
            System.out.println("✓ PASS: All errors corrected successfully");
        } else {
            System.out.println("✗ FAIL: Some errors not corrected");
        }
        System.out.println();
    }
}
