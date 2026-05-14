package test;

import decode.util.ReedSolomonSimple;

public class TestRSSimple {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Reed-Solomon Simple Implementation Test ===\n");

        // Test GF(256) arithmetic
        System.out.println("Test 1: GF(256) Multiplication");
        System.out.println("  0x02 * 0x02 = 0x" + String.format("%02X", ReedSolomonSimple.gfMult(0x02, 0x02)) +
                          " (expect 0x04)");
        System.out.println("  0x03 * 0x04 = 0x" + String.format("%02X", ReedSolomonSimple.gfMult(0x03, 0x04)) +
                          " (expect 0x0C)");
        System.out.println("  0x80 * 0x02 = 0x" + String.format("%02X", ReedSolomonSimple.gfMult(0x80, 0x02)) +
                          " (expect 0x1D)");
        System.out.println("  0x53 * 0xCA = 0x" + String.format("%02X", ReedSolomonSimple.gfMult(0x53, 0xCA)) +
                          " (expect 0x01)");

        // Test division
        System.out.println("\nTest 2: GF(256) Division");
        System.out.println("  0x04 / 0x02 = 0x" + String.format("%02X", ReedSolomonSimple.gfDiv(0x04, 0x02)) +
                          " (expect 0x02)");

        // Test perfect block syndrome
        System.out.println("\nTest 3: Perfect Block Syndrome");
        byte[] block = new byte[255];
        for (int i = 0; i < 239; i++) {
            block[i] = (byte)(i & 0xFF);
        }
        // Parity bytes at 239-254 (all zeros for this test)

        System.out.println("  Computing syndromes...");
        int syndromeCount = 0;
        for (int i = 0; i < 16; i++) {
            // Note: In real RS, syndromes are computed using generator polynomial roots
            // For now, just show that we can evaluate polynomials
            int alpha = 2 + i;  // arbitrary test points
            int val = ReedSolomonSimple.evaluate(block, alpha);
            if (val == 0) syndromeCount++;
        }
        System.out.println("  Syndromes with value 0: " + syndromeCount + "/16");

        // Test real R2007 data
        System.out.println("\nTest 4: Real R2007 Arc.dwg");
        try {
            byte[] fileData = java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get("samples/2007/Arc.dwg"));

            if (fileData.length >= 0x80 + 765) {
                byte[] rsData = new byte[765];
                System.arraycopy(fileData, 0x80, rsData, 0, 765);

                byte[] decoded = ReedSolomonSimple.decodeR2007Data(rsData);
                if (decoded != null) {
                    System.out.println("  ✓ Decoded 717 bytes");
                    System.out.println("  First 20 bytes: " + formatBytes(decoded, 0, 20));

                    // Check if it looks like real data
                    java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(decoded);
                    bb.order(java.nio.ByteOrder.LITTLE_ENDIAN);
                    long headerSize = bb.getLong(0);
                    System.out.println("  headerSize = 0x" + Long.toHexString(headerSize));
                } else {
                    System.out.println("  ✗ Decoding failed");
                }
            } else {
                System.out.println("  File too small");
            }
        } catch (Exception e) {
            System.out.println("  Error: " + e.getMessage());
        }
    }

    private static String formatBytes(byte[] data, int offset, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < offset + count && i < data.length; i++) {
            sb.append(String.format("%02X ", data[i] & 0xFF));
        }
        return sb.toString();
    }
}
