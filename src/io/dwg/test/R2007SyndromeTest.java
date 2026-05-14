package io.dwg.test;

import io.dwg.core.util.ReedSolomonDecoder;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;

/**
 * Test syndrome calculation on real R2007 data
 */
public class R2007SyndromeTest {
    public static void main(String[] args) throws Exception {
        System.out.println("=== R2007 Syndrome Calculation Test ===\n");

        // Find the test file
        String[] possiblePaths = {
            "samples/2007/Line.dwg",
            "../samples/2007/Line.dwg"
        };

        String filePath = null;
        for (String path : possiblePaths) {
            File f = new File(path);
            if (f.exists()) {
                filePath = path;
                break;
            }
        }

        if (filePath == null) {
            System.out.println("ERROR: Test file not found");
            return;
        }

        System.out.println("Reading file: " + filePath);
        byte[] data = Files.readAllBytes(new File(filePath).toPath());

        // Extract RS-encoded data (984 bytes at offset 0x80 per libredwg)
        byte[] rsData = new byte[984];
        System.arraycopy(data, 0x80, rsData, 0, 984);

        // Get the first block
        byte[] block1 = new byte[255];
        System.arraycopy(rsData, 0, block1, 0, 255);

        System.out.println("\n=== Block 1 Analysis ===");
        System.out.printf("First 32 bytes: ");
        for (int i = 0; i < 32; i++) {
            System.out.printf("%02x ", block1[i] & 0xFF);
        }
        System.out.println();

        // Get reflection methods
        Method evaluate = getPrivateMethod(ReedSolomonDecoder.class, "evaluate", byte[].class, int.class, int.class);
        int[] F256_POWER = getStaticField(ReedSolomonDecoder.class, "F256_POWER");

        if (evaluate == null) {
            System.out.println("ERROR: Could not find evaluate() method");
            return;
        }

        // Calculate syndromes
        System.out.println("\n=== Syndromes ===");
        byte[] syndromes = new byte[16];
        boolean hasError = false;

        for (int j = 0; j < 16; j++) {
            int val = (int) evaluate.invoke(null, block1, 254, F256_POWER[j + 1] & 0xFF);
            syndromes[j] = (byte) val;
            System.out.printf("S[%d] = 0x%02x\n", j, syndromes[j] & 0xFF);
            if ((syndromes[j] & 0xFF) != 0) {
                hasError = true;
            }
        }

        System.out.println("\nHas error: " + hasError);

        if (hasError) {
            System.out.println("\n=== Syndrome Analysis ===");

            // Count non-zero syndromes
            int nonZeroCount = 0;
            for (int j = 0; j < 16; j++) {
                if ((syndromes[j] & 0xFF) != 0) {
                    nonZeroCount++;
                }
            }
            System.out.printf("Non-zero syndromes: %d out of 16\n", nonZeroCount);

            // Check if syndromes follow a pattern
            System.out.println("\nSyndrome values (non-zero only):");
            for (int j = 0; j < 16; j++) {
                if ((syndromes[j] & 0xFF) != 0) {
                    System.out.printf("  S[%d] = 0x%02x (%d)\n", j, syndromes[j] & 0xFF, syndromes[j] & 0xFF);
                }
            }
        }

        System.out.println("\n=== Conclusion ===");
        if (!hasError) {
            System.out.println("Block 1 appears to be clean (all syndromes are zero).");
            System.out.println("This could mean:");
            System.out.println("  1. The data is correctly RS-encoded with no errors");
            System.out.println("  2. The block is pure information with CRC/padding");
        } else {
            System.out.println("Block 1 has detectable errors.");
            System.out.println("This could mean:");
            System.out.println("  1. The data is corrupted");
            System.out.println("  2. The data is not RS-encoded in this location");
            System.out.println("  3. The RS encoding algorithm has a different structure");
        }
    }

    private static Method getPrivateMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static int[] getStaticField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (int[]) field.get(null);
        } catch (Exception e) {
            return null;
        }
    }
}
