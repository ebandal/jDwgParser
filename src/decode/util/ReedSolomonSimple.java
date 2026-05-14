package decode.util;

/**
 * Simplified Reed-Solomon(255,239) Decoder
 *
 * Based on polynomial long division approach.
 * More transparent than matrix-based Berlekamp-Massey.
 */
public class ReedSolomonSimple {

    // Primitive polynomial: x^8 + x^4 + x^3 + x^2 + 1 = 0x11D
    private static final int POLY = 0x11D;

    // Precomputed GF(256) tables
    private static final int[] EXP_TABLE = new int[256];
    private static final int[] LOG_TABLE = new int[256];

    static {
        // Build Exp and Log tables
        int val = 1;
        for (int i = 0; i < 256; i++) {
            EXP_TABLE[i] = val;
            LOG_TABLE[val] = i;
            val <<= 1;
            if (val > 255) {
                val ^= POLY;
            }
        }
        // Set LOG_TABLE[0] = 0 (undefined, but needs a value)
        LOG_TABLE[0] = 0;
    }

    /**
     * Multiply two GF(256) elements using exp/log tables
     */
    public static int gfMult(int a, int b) {
        if (a == 0 || b == 0) return 0;
        return EXP_TABLE[(LOG_TABLE[a] + LOG_TABLE[b]) % 255];
    }

    /**
     * Divide GF(256) element a by b
     */
    public static int gfDiv(int a, int b) {
        if (b == 0) throw new ArithmeticException("Division by zero in GF(256)");
        if (a == 0) return 0;
        return EXP_TABLE[(LOG_TABLE[a] - LOG_TABLE[b] + 255) % 255];
    }

    /**
     * Add two GF(256) polynomials (coefficients)
     */
    public static int gfAdd(int a, int b) {
        return a ^ b;
    }

    /**
     * Evaluate polynomial at x using Horner's method
     */
    public static int evaluate(byte[] poly, int x) {
        int y = 0;
        for (int i = poly.length - 1; i >= 0; i--) {
            y = gfMult(y, x) ^ (poly[i] & 0xFF);
        }
        return y;
    }

    /**
     * Decode a 255-byte RS(255,239) block
     * Returns number of errors corrected (-1 if uncorrectable)
     */
    public static int decodeBlock(byte[] block) {
        if (block == null || block.length != 255) {
            return -1;
        }

        // Compute syndromes
        byte[] syn = new byte[16];
        boolean hasError = false;

        for (int i = 0; i < 16; i++) {
            int alpha = EXP_TABLE[i + 1];  // Evaluation point: α^(i+1)
            syn[i] = (byte) evaluate(block, alpha);
            if (syn[i] != 0) hasError = true;
        }

        // No errors detected
        if (!hasError) {
            return 0;
        }

        // For now: return error indication (full BM algorithm would go here)
        // This is where we'd implement Berlekamp-Massey or Euclidean algorithm
        // Simplified: just return that we have errors but can't correct yet
        return -1;
    }

    /**
     * Decode R2007 header (3 × 255-byte blocks → 717 bytes)
     */
    public static byte[] decodeR2007Data(byte[] data) {
        if (data == null || data.length < 765) {
            return null;
        }

        byte[] result = new byte[717];

        try {
            byte[] block1 = new byte[255];
            byte[] block2 = new byte[255];
            byte[] block3 = new byte[255];

            System.arraycopy(data, 0, block1, 0, 255);
            System.arraycopy(data, 255, block2, 0, 255);
            System.arraycopy(data, 510, block3, 0, 255);

            // For now: just return the data (assuming no heavy errors)
            System.arraycopy(block1, 0, result, 0, 239);
            System.arraycopy(block2, 0, result, 239, 239);
            System.arraycopy(block3, 0, result, 478, 239);

            return result;
        } catch (Exception e) {
            return null;
        }
    }
}
