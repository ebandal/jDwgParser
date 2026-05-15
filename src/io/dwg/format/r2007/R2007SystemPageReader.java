package io.dwg.format.r2007;

import io.dwg.core.io.BitInput;
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
     * Decode RS blocks from file data.
     * Match libredwg decode_rs() exactly: deinterleave only 239 data bytes per block,
     * skip the 16 parity bytes, and do NOT run RS error correction.
     * libredwg has rs_decode_block() commented out because BM produces incorrect
     * "corrections" on valid R2010+ data.
     */
    private static byte[] decodeRSBlocks(byte[] rsData, long blockCount) {
        if (blockCount < 0 || blockCount > Integer.MAX_VALUE) {
            return null;
        }

        int blockCountInt = (int) blockCount;
        final int DATA_SIZE = 239;

        byte[] result = new byte[blockCountInt * DATA_SIZE];
        for (int i = 0; i < blockCountInt; i++) {
            for (int j = 0; j < DATA_SIZE; j++) {
                int srcOffset = i + j * blockCountInt;
                if (srcOffset < rsData.length) {
                    result[i * DATA_SIZE + j] = rsData[srcOffset];
                }
            }
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
