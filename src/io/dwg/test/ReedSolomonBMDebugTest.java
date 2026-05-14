package io.dwg.test;

import io.dwg.core.util.ReedSolomonDecoder;
import java.lang.reflect.*;
import java.util.*;

/**
 * Debug test for Berlekamp-Massey algorithm
 */
public class ReedSolomonBMDebugTest {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Berlekamp-Massey Algorithm Debug ===\n");

        // Get methods using reflection
        Method evaluate = getPrivateMethod(ReedSolomonDecoder.class, "evaluate", byte[].class, int.class, int.class);
        Method f256Multiply = getPrivateMethod(ReedSolomonDecoder.class, "f256Multiply", int.class, int.class);
        Method degree = getPrivateMethod(ReedSolomonDecoder.class, "degree", byte[].class);
        Method solveKeyEquation = getPrivateMethod(ReedSolomonDecoder.class, "solveKeyEquation",
                                                   byte[].class, byte[].class, byte[].class);

        if (solveKeyEquation == null) {
            System.out.println("ERROR: Could not find solveKeyEquation() method");
            return;
        }

        // Test case: All-zero syndromes (no errors)
        System.out.println("Test 1: All-zero syndromes (no errors)");
        byte[] syndromes1 = new byte[16];  // All zeros
        byte[] sigma1 = new byte[32];
        byte[] omega1 = new byte[32];

        solveKeyEquation.invoke(null, syndromes1, sigma1, omega1);

        int sigmaDeg1 = (int) degree.invoke(null, sigma1);
        int omegaDeg1 = (int) degree.invoke(null, omega1);
        System.out.printf("  Result: sigma degree=%d (expected 0), omega degree=%d (expected <=0)\n",
                         sigmaDeg1, omegaDeg1);
        System.out.printf("  sigma[0]=0x%02x (expected 0x01 for [1] polynomial)\n", sigma1[0] & 0xFF);

        // In this case, sigma should be the constant polynomial [1, 0, 0, ...]
        // (error locator polynomial with no roots = no errors)
        boolean test1Pass = (sigma1[0] & 0xFF) == 1 && sigmaDeg1 <= 0;
        System.out.println("  PASS: " + test1Pass + "\n");

        // Test case: Known syndrome from a specific error pattern
        System.out.println("Test 2: Single error at position 100");
        // If there's a single error of value 0x50 at position 100:
        // The syndrome calculation would be complex, so let's create test data manually

        // Create a synthetic block with a single error
        byte[] testBlock = new byte[255];
        Arrays.fill(testBlock, (byte) 0x00);  // Clean block
        testBlock[100] = (byte) 0x50;  // Error at position 100

        // Calculate syndromes for this block
        byte[] syndromes2 = new byte[16];
        int[] F256_POWER = getStaticField(ReedSolomonDecoder.class, "F256_POWER");

        for (int j = 0; j < 16; j++) {
            int val = (int) evaluate.invoke(null, testBlock, 254, F256_POWER[j + 1] & 0xFF);
            syndromes2[j] = (byte) val;
        }

        System.out.printf("  Syndromes: ");
        boolean hasError = false;
        for (int j = 0; j < 16; j++) {
            System.out.printf("%02x ", syndromes2[j] & 0xFF);
            if ((syndromes2[j] & 0xFF) != 0) hasError = true;
        }
        System.out.println();
        System.out.println("  Has error: " + hasError);

        if (hasError) {
            byte[] sigma2 = new byte[32];
            byte[] omega2 = new byte[32];
            solveKeyEquation.invoke(null, syndromes2, sigma2, omega2);

            int sigmaDeg2 = (int) degree.invoke(null, sigma2);
            int omegaDeg2 = (int) degree.invoke(null, omega2);

            System.out.printf("  Result: sigma degree=%d (expected 1 for 1 error), omega degree=%d\n",
                             sigmaDeg2, omegaDeg2);

            System.out.println("  sigma coefficients:");
            for (int i = 0; i <= Math.min(8, sigmaDeg2); i++) {
                System.out.printf("    [%d]=0x%02x\n", i, sigma2[i] & 0xFF);
            }

            System.out.println("  omega coefficients:");
            for (int i = 0; i <= Math.min(8, omegaDeg2); i++) {
                System.out.printf("    [%d]=0x%02x\n", i, omega2[i] & 0xFF);
            }

            // For a single error, sigma should be degree 1: [something, something]
            boolean test2Pass = sigmaDeg2 == 1;
            System.out.println("  PASS: " + test2Pass + "\n");
        } else {
            System.out.println("  WARNING: Expected errors not detected!\n");
        }

        System.out.println("=== Summary ===");
        System.out.println("BM algorithm test complete. Check above for correctness.");
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

    private static int[] getStaticField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (int[]) field.get(null);
        } catch (Exception e) {
            System.err.println("ERROR: Field not found: " + fieldName);
            return null;
        }
    }
}
