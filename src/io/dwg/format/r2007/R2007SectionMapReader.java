package io.dwg.format.r2007;

import io.dwg.core.io.BitInput;
import io.dwg.core.util.ReedSolomonDecoder;
import io.dwg.core.util.Lz77Decompressor;

/**
 * Read and decompress R2007 SectionMap (similar to PageMap but larger)
 */
public class R2007SectionMapReader {

    /**
     * Read and decompress SectionMap from file
     * Uses same pipeline as PageMap but with potentially more blocks
     */
    public static byte[] readSectionMap(BitInput input, long fileOffset,
                                        long sizeComp, long sizeUncomp,
                                        long repeatCount) throws Exception {
        if (sizeComp < 0 || sizeUncomp < 0) {
            throw new Exception("Invalid section map sizes");
        }

        // Calculate RS encoding parameters (identical to system pages)
        long pesize = ((sizeComp + 7) & ~7L) * repeatCount;
        long blockCount = (pesize + 238) / 239;
        long pageSize = (blockCount * 255 + 7) & ~7L;

        if (blockCount <= 0 || pageSize <= 0) {
            throw new Exception(String.format("Invalid section map: sizeComp=%d, sizeUncomp=%d, pageSize=%d",
                sizeComp, sizeUncomp, pageSize));
        }

        // Read RS-encoded data from file
        input.seek(fileOffset * 8);
        byte[] rsData = new byte[(int)(Math.min(pageSize, Integer.MAX_VALUE))];
        for (int i = 0; i < rsData.length && !input.isEof(); i++) {
            rsData[i] = (byte) input.readRawChar();
        }

        // RS decode: blockCount blocks of 255 bytes each
        byte[] pedata = decodeRSBlocks(rsData, blockCount);
        if (pedata == null) {
            throw new Exception("RS decoding failed for section map");
        }

        // LZ77 decompress if needed
        byte[] decompressed;
        if (sizeComp < sizeUncomp) {
            Lz77Decompressor lz77 = new Lz77Decompressor();
            decompressed = lz77.decompress(pedata, (int)(Math.min(sizeUncomp, Integer.MAX_VALUE)));
        } else {
            decompressed = new byte[(int)Math.min(sizeUncomp, Integer.MAX_VALUE)];
            System.arraycopy(pedata, 0, decompressed, 0, decompressed.length);
        }

        return decompressed;
    }

    /**
     * Decode RS blocks from file data (interleaved)
     */
    private static byte[] decodeRSBlocks(byte[] rsData, long blockCount) {
        if (blockCount < 0 || blockCount > Integer.MAX_VALUE) {
            return null;
        }

        int blockCountInt = (int) blockCount;
        byte[][] blocks = new byte[blockCountInt][255];

        // Deinterleave: blocks are stored interleaved byte-by-byte
        for (int i = 0; i < blockCountInt; i++) {
            for (int j = 0; j < 255; j++) {
                int srcOffset = i + j * blockCountInt;
                if (srcOffset < rsData.length) {
                    blocks[i][j] = rsData[srcOffset];
                }
            }
        }

        System.out.println("[DEBUG] Section map RS decode: blockCount=" + blockCountInt + " (interleaved)");

        // Decode each block
        for (int i = 0; i < blockCountInt; i++) {
            int errors = ReedSolomonDecoder.decodeBlock(blocks[i], true);
            if (errors < 0) {
                System.out.println("[WARN] RS decode failed for section map block " + i);
            }
        }

        // Extract 239 bytes from each block
        byte[] result = new byte[blockCountInt * 239];
        for (int i = 0; i < blockCountInt; i++) {
            System.arraycopy(blocks[i], 0, result, i * 239, 239);
        }

        return result;
    }
}
