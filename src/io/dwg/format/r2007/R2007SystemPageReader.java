package io.dwg.format.r2007;

import io.dwg.core.io.BitInput;
import io.dwg.core.util.ReedSolomonDecoder;
import io.dwg.core.util.Lz77Decompressor;

/**
 * Read and decompress R2007 system pages (PageMap, SectionMap)
 * Based on libredwg's read_system_page() function
 */
public class R2007SystemPageReader {

    /**
     * Read and decompress a system page from file
     * Returns decompressed data ready for parsing
     */
    public static byte[] readSystemPage(BitInput input, long fileOffset,
                                        long sizeComp, long sizeUncomp,
                                        long repeatCount) throws Exception {
        if (sizeComp < 0 || sizeUncomp < 0) {
            throw new Exception("Invalid system page sizes");
        }

        // Calculate RS encoding parameters (per libredwg)
        long pesize = ((sizeComp + 7) & ~7L) * repeatCount;  // Round to multiple of 8
        long blockCount = (pesize + 238) / 239;              // Divide by RS k-value
        long pageSize = (blockCount * 255 + 7) & ~7L;        // Multiply by RS n, round to 8

        if (blockCount <= 0 || pageSize <= 0) {
            throw new Exception(String.format("Invalid system page: sizeComp=%d, sizeUncomp=%d, pageSize=%d",
                sizeComp, sizeUncomp, pageSize));
        }

        // Read RS-encoded data from file
        input.seek(fileOffset * 8);
        byte[] rsData = new byte[(int)(Math.min(pageSize, Integer.MAX_VALUE))];
        for (int i = 0; i < rsData.length && !input.isEof(); i++) {
            rsData[i] = (byte) input.readRawChar();
        }

        // RS decode: blockCount blocks of 255 bytes each → 239 bytes data per block
        byte[] pedata = decodeRSBlocks(rsData, blockCount);
        if (pedata == null) {
            throw new Exception("RS decoding failed for system page");
        }

        // LZ77 decompress if needed
        byte[] decompressed;
        if (sizeComp < sizeUncomp) {
            Lz77Decompressor lz77 = new Lz77Decompressor();
            decompressed = lz77.decompress(pedata, (int)(Math.min(sizeUncomp, Integer.MAX_VALUE)));
        } else {
            // Not compressed, use as-is
            decompressed = new byte[(int)Math.min(sizeUncomp, Integer.MAX_VALUE)];
            System.arraycopy(pedata, 0, decompressed, 0, decompressed.length);
        }

        return decompressed;
    }

    /**
     * Decode RS blocks from file data
     * For system pages with blockCount blocks (stored INTERLEAVED byte-by-byte, like R2007 header)
     * Input: blockCount * 255 bytes, where blocks are interleaved:
     *        data[0 + j*blockCount], data[1 + j*blockCount], ..., data[blockCount-1 + j*blockCount]
     * Output: blockCount * 239 bytes
     */
    private static byte[] decodeRSBlocks(byte[] rsData, long blockCount) {
        if (blockCount < 0 || blockCount > Integer.MAX_VALUE) {
            return null;
        }

        int blockCountInt = (int) blockCount;
        byte[][] blocks = new byte[blockCountInt][255];

        // Deinterleave: R2007 system pages are stored with blocks interleaved byte-by-byte
        // Same pattern as R2007 header: data[i + j * blockCount] contains block[i][j]
        for (int i = 0; i < blockCountInt; i++) {
            for (int j = 0; j < 255; j++) {
                int srcOffset = i + j * blockCountInt;
                if (srcOffset < rsData.length) {
                    blocks[i][j] = rsData[srcOffset];
                }
            }
        }

        System.out.println("[DEBUG] System page RS decode: blockCount=" + blockCountInt + " (interleaved)");

        // Decode each block using ReedSolomonDecoder
        for (int i = 0; i < blockCountInt; i++) {
            int errors = ReedSolomonDecoder.decodeBlock(blocks[i], true);
            if (errors < 0) {
                System.out.println("[WARN] RS decode failed for block " + i);
                // Continue anyway; some blocks may fail
            } else {
                System.out.println("[DEBUG]   Block " + i + ": " + errors + " errors corrected");
            }
        }

        // Extract 239 bytes from each block and concatenate
        byte[] result = new byte[blockCountInt * 239];
        for (int i = 0; i < blockCountInt; i++) {
            System.arraycopy(blocks[i], 0, result, i * 239, 239);
        }

        return result;
    }

    /**
     * Simple helper to read LE64 value for RLL parsing
     */
    public static long readRLL(byte[] data, int[] offsetHolder) throws Exception {
        int offset = offsetHolder[0];
        if (offset + 8 > data.length) {
            throw new Exception("RLL read out of bounds");
        }

        // Read as little-endian 64-bit value
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value |= ((long)(data[offset + i] & 0xFF)) << (i * 8);
        }

        offsetHolder[0] += 8;
        return value;
    }
}
