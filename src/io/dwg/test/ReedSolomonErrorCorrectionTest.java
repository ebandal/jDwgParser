package io.dwg.test;

import io.dwg.core.util.ReedSolomonDecoder;
import java.lang.reflect.*;
import java.util.*;

/**
 * Debug test for error correction (Chien search + Forney)
 */
public class ReedSolomonErrorCorrectionTest {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Reed-Solomon Error Correction Debug ===\n");

        // Get methods using reflection
        Method decodeBlock = getPrivateMethod(ReedSolomonDecoder.class, "decodeBlock", byte[].class, boolean.class);

        if (decodeBlock == null) {
            System.out.println("ERROR: Could not find decodeBlock() method");
            return;
        }

        // Test 1: Clean block (no errors)
        System.out.println("Test 1: Clean block (no errors)");
        byte[] cleanBlock = new byte[255];
        Arrays.fill(cleanBlock, (byte) 0x00);

        int result1 = (int) decodeBlock.invoke(null, cleanBlock, false);
        System.out.printf("  Result: %d (expected 0 for no errors)\n", result1);
        System.out.println("  PASS: " + (result1 == 0) + "\n");

        // Test 2: Block with single error
        System.out.println("Test 2: Block with single error (no correction)");
        byte[] errorBlock = new byte[255];
        Arrays.fill(errorBlock, (byte) 0x00);
        errorBlock[100] = (byte) 0x50;  // Introduce error

        int result2 = (int) decodeBlock.invoke(null, errorBlock, false);
        System.out.printf("  Result: %d (expected -1 for unrecoverable without fix)\n", result2);
        System.out.println("  PASS: " + (result2 == -1) + "\n");

        // Test 3: Same block with correction attempt
        System.out.println("Test 3: Block with single error (with correction)");
        byte[] errorBlock2 = new byte[255];
        Arrays.fill(errorBlock2, (byte) 0x00);
        errorBlock2[100] = (byte) 0x50;  // Introduce error

        System.out.println("  [Attempting to correct error with Berlekamp-Massey...]");
        int result3 = (int) decodeBlock.invoke(null, errorBlock2, true);
        System.out.printf("  Result: %d (>0 = corrected, -1 = uncorrectable)\n", result3);

        if (result3 > 0) {
            System.out.printf("  Correction successful! %d error(s) fixed.\n", result3);
            System.out.printf("  Block[100] after correction: 0x%02x (expected 0x00)\n", errorBlock2[100] & 0xFF);
            System.out.println("  PASS: " + (errorBlock2[100] == 0x00));
        } else if (result3 == 0) {
            System.out.println("  WARNING: Reported no errors even though block was modified?");
            System.out.println("  PASS: false");
        } else {
            System.out.println("  ERROR: Unable to correct error (returned -1)");
            System.out.println("  PASS: false");
        }
        System.out.println();

        // Test 4: Create realistic test data
        System.out.println("Test 4: Sequential data pattern");
        byte[] seqBlock = new byte[255];
        for (int i = 0; i < 255; i++) {
            seqBlock[i] = (byte) i;
        }

        int result4 = (int) decodeBlock.invoke(null, seqBlock, false);
        System.out.printf("  Result: %d\n", result4);
        if (result4 == 0) {
            System.out.println("  Block is clean (all data follows RS pattern)");
        } else if (result4 == -1) {
            System.out.println("  Block has errors (not RS-encoded)");
        }
        System.out.println();

        System.out.println("=== Summary ===");
        System.out.println("Check above to see if error correction works.");
        System.out.println("Key issue to look for: Can Chien search find error roots?");
    }

    private static Method getPrivateMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            System.err.println("ERROR: Method not found: " + methodName);
            return null;
        }
    }
}
