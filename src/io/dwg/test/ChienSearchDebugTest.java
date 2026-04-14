package io.dwg.test;

import io.dwg.core.util.ReedSolomonDecoder;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;

/**
 * Debug Chien search algorithm
 */
public class ChienSearchDebugTest {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Chien Search Debug ===\n");

        // Create simple test block with known error
        byte[] block = new byte[255];
        for (int i = 0; i < 255; i++) {
            block[i] = 0;
        }
        block[100] = (byte) 0x50;  // Single error at position 100

        System.out.println("Test block: 255 zeros with error 0x50 at position 100");

        // Get reflection methods
        Method evaluate = getPrivateMethod(ReedSolomonDecoder.class, "evaluate", byte[].class, int.class, int.class);
        Method degree = getPrivateMethod(ReedSolomonDecoder.class, "degree", byte[].class);
        Method solveKeyEquation = getPrivateMethod(ReedSolomonDecoder.class, "solveKeyEquation",
                                                   byte[].class, byte[].class, byte[].class);
        Method fixErrors = getPrivateMethod(ReedSolomonDecoder.class, "fixErrors",
                                           byte[].class, byte[].class, byte[].class);
        int[] F256_POWER = getStaticField(ReedSolomonDecoder.class, "F256_POWER");

        // Calculate syndromes
        System.out.println("\n=== Syndrome Calculation ===");
        byte[] syndromes = new byte[16];
        for (int j = 0; j < 16; j++) {
            int val = (int) evaluate.invoke(null, block, 254, F256_POWER[j + 1] & 0xFF);
            syndromes[j] = (byte) val;
            System.out.printf("S[%d] = 0x%02x\n", j, syndromes[j] & 0xFF);
        }

        // Solve key equation
        System.out.println("\n=== Berlekamp-Massey Algorithm ===");
        byte[] sigma = new byte[32];
        byte[] omega = new byte[32];
        solveKeyEquation.invoke(null, syndromes, sigma, omega);

        int sigmaDeg = (int) degree.invoke(null, sigma);
        int omegaDeg = (int) degree.invoke(null, omega);
        System.out.printf("sigma degree: %d\n", sigmaDeg);
        System.out.printf("omega degree: %d\n", omegaDeg);

        System.out.println("sigma[0.." + sigmaDeg + "]:");
        for (int i = 0; i <= sigmaDeg && i < 32; i++) {
            System.out.printf("  [%d] = 0x%02x\n", i, sigma[i] & 0xFF);
        }

        // Manually do Chien search
        System.out.println("\n=== Chien Search (Manual) ===");
        int nerr = 0;
        int[] roots = new int[8];

        for (int x = 0; x < 256; x++) {
            int val = (int) evaluate.invoke(null, sigma, sigmaDeg, x);
            if (val == 0) {
                System.out.printf("Found root at x=%d (0x%02x)\n", x, x);
                roots[nerr++] = x;
            }
        }

        System.out.printf("\nTotal roots found: %d (expected %d)\n", nerr, sigmaDeg);

        if (nerr != sigmaDeg) {
            System.out.println("ERROR: nerr != sigmaDeg (Chien search failed!)");
        } else {
            System.out.println("OK: Found correct number of roots");
        }

        // Try fixErrors
        System.out.println("\n=== Error Correction (fixErrors) ===");
        byte[] testBlock = new byte[255];
        for (int i = 0; i < 255; i++) {
            testBlock[i] = block[i];  // Copy original
        }

        int result = (int) fixErrors.invoke(null, testBlock, sigma, omega);
        System.out.printf("fixErrors result: %d\n", result);

        if (result > 0) {
            System.out.println("SUCCESS: Errors corrected");
            System.out.printf("Block[100] after correction: 0x%02x (expected 0x00)\n", testBlock[100] & 0xFF);
        } else if (result == -1) {
            System.out.println("FAILED: Unable to correct errors");
            System.out.println("This means Chien search found nerr != sigmaDeg");
        }
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
