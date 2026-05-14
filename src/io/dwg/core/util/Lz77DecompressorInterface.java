package io.dwg.core.util;

/**
 * Common interface for LZ77 decompressors.
 * Both R2004 and R2007+ use similar LZ77 format (byte-level).
 */
public interface Lz77DecompressorInterface {
    byte[] decompress(byte[] compressed, int expectedSize) throws Exception;
}
