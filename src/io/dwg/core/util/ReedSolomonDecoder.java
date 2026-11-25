package io.dwg.core.util;

/**
 * Removes the Reed-Solomon encoding of R2007 (AC1021) files.
 *
 * <p>Written from the Open Design Specification for .dwg files, section 5.13 "Reed-Solomon
 * encoding". R2007 encodes data in code words of {@value #CODEWORD_SIZE} bytes:
 * <ul>
 *   <li>system pages and the file header: {@value #SYSTEM_DATA_SIZE} data bytes and 16 parity
 *       bytes per code word;</li>
 *   <li>data pages: {@value #DATA_PAGE_DATA_SIZE} data bytes and 4 parity bytes per code
 *       word.</li>
 * </ul>
 * When there are several code words they are interleaved (section 5.13.2): byte {@code j} of
 * code word {@code i} is stored at position {@code j * blockCount + i}.
 *
 * <p>This class only takes the data bytes out of the code words; it does not use the parity bytes
 * to correct errors.
 */
public final class ReedSolomonDecoder {

    /** Size of a code word (n). */
    public static final int CODEWORD_SIZE = 255;
    /** Data bytes per code word of system pages and the file header (k). */
    public static final int SYSTEM_DATA_SIZE = 239;
    /** Data bytes per code word of data pages (k). */
    public static final int DATA_PAGE_DATA_SIZE = 251;

    /** Number of code words in the encoded file header. */
    private static final int HEADER_BLOCK_COUNT = 3;

    private ReedSolomonDecoder() {
    }

    /**
     * Takes the data bytes out of {@code blockCount} interleaved code words.
     *
     * @param encoded    the encoded bytes; positions past its end are read as zero
     * @param blockCount the number of code words
     * @param dataSize   the data bytes per code word (k)
     * @return {@code blockCount * dataSize} bytes: the data of code word 0, then of code word 1, ...
     */
    public static byte[] deinterleave(byte[] encoded, int blockCount, int dataSize) {
        byte[] data = new byte[blockCount * dataSize];
        for (int block = 0; block < blockCount; block++) {
            int to = block * dataSize;
            for (int j = 0, from = block; j < dataSize; j++, from += blockCount) {
                if (from < encoded.length) {
                    data[to + j] = encoded[from];
                }
            }
        }
        return data;
    }

    /**
     * Decodes the Reed-Solomon encoded part of the R2007 file header (3 interleaved system code
     * words).
     *
     * @return {@code 3 * 239} bytes, or null if {@code data} is shorter than 3 code words
     */
    public static byte[] decodeR2007Data(byte[] data) {
        if (data == null || data.length < HEADER_BLOCK_COUNT * CODEWORD_SIZE) {
            return null;
        }
        return deinterleave(data, HEADER_BLOCK_COUNT, SYSTEM_DATA_SIZE);
    }
}
