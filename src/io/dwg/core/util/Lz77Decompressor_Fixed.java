package io.dwg.core.util;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;

/**
 * Fixed R2007+ LZ77 Decompressor based on libredwg algorithm
 * 
 * DWG uses a modified LZ77 with:
 * - Variable-length literal/backref lengths
 * - 15-bit offsets
 * - Interleaved bit-level encoding
 */
public class Lz77Decompressor_Fixed {
    private static final int WINDOW_SIZE = 0x8000;  // 32KB

    public byte[] decompress(byte[] compressed, int expectedSize) throws Exception {
        byte[] output = new byte[expectedSize];
        BitInput in = new ByteBufferBitInput(compressed);
        int outPos = 0;

        while (outPos < expectedSize && !in.isEof()) {
            boolean isLiteral = in.readBit();
            
            if (isLiteral) {
                // Literal run
                int length = readLength(in, 4, 15);
                for (int i = 0; i < length && outPos < expectedSize; i++) {
                    output[outPos++] = (byte) in.readRawChar();
                }
            } else {
                // Back reference
                int offset = in.readBits(15) + 1;  // +1 because 0 is invalid
                int length = readLength(in, 4, 15) + 2;  // min length 2
                
                // Copy from history
                int refPos = outPos - offset;
                if (refPos >= 0) {
                    for (int i = 0; i < length && outPos < expectedSize; i++) {
                        output[outPos++] = output[refPos + i];
                    }
                }
            }
        }

        return output;
    }

    /**
     * Read variable-length encoded integer.
     * Format: first N bits, if all 1s then read full byte and add to N*2-1
     */
    private int readLength(BitInput in, int initialBits, int maxValue) throws Exception {
        int length = in.readBits(initialBits);
        
        if (length == ((1 << initialBits) - 1)) {
            // All bits were 1, read full byte and add
            byte ext = (byte) in.readRawChar();
            length = (ext & 0xFF) + maxValue;
        }
        
        return length;
    }
}
