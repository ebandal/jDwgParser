package io.dwg.core.util;

/**
 * Wrapper for byte-level LZ77 decompressor (used by both R2004 and R2007+).
 * Implements the interface expected by R2007FileStructureHandler.
 */
public class ByteLevelLz77Decompressor implements Lz77DecompressorInterface {

    @Override
    public byte[] decompress(byte[] compressed, int expectedSize) throws Exception {
        return decode.util.R2004Lz77.decompress(compressed, expectedSize);
    }
}
