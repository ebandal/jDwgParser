package io.dwg.format.r2007;

import io.dwg.core.io.BitInput;
import io.dwg.core.util.Lz77Decompressor;
import io.dwg.core.util.ReedSolomonDecoder;

/**
 * Reads an R2007 system page: the page map or the section map.
 *
 * <p>Written from the Open Design Specification for .dwg files, section 5.3 "System section page".
 * When the page is written, its data (compressed or not) is padded with zeros to a multiple of 8
 * bytes, repeated {@code repeatCount} times, and Reed-Solomon encoded as interleaved system code
 * words (239 data bytes each). Reading reverses this: take the data bytes out of the code words,
 * keep the first copy, and decompress it if it is compressed.
 */
public class R2007SystemPageReader {

    private static final int CRC_BLOCK_SIZE = 8;

    /**
     * Reads and decodes a system page.
     *
     * @param input       the whole file
     * @param fileOffset  the byte offset of the page in the file
     * @param sizeComp    the size of the stored (compressed) data
     * @param sizeUncomp  the size of the data after decompression
     * @param repeatCount how many times the data is repeated in the page (the "correction" or
     *                    "data factor" of the file header)
     * @return the page data, {@code sizeUncomp} bytes
     */
    public static byte[] readSystemPage(BitInput input, long fileOffset,
                                        long sizeComp, long sizeUncomp,
                                        long repeatCount) throws Exception {
        if (sizeComp < 0 || sizeUncomp < 0 || sizeUncomp > Integer.MAX_VALUE) {
            throw new Exception("Invalid system page sizes");
        }

        long alignedSize = (sizeComp + CRC_BLOCK_SIZE - 1) & -CRC_BLOCK_SIZE;
        long encodedDataSize = alignedSize * repeatCount;
        long blockCount = (encodedDataSize + ReedSolomonDecoder.SYSTEM_DATA_SIZE - 1)
            / ReedSolomonDecoder.SYSTEM_DATA_SIZE;
        if (blockCount <= 0 || blockCount * ReedSolomonDecoder.CODEWORD_SIZE > Integer.MAX_VALUE) {
            throw new Exception(String.format(
                "Invalid system page: sizeComp=%d, sizeUncomp=%d, repeatCount=%d",
                sizeComp, sizeUncomp, repeatCount));
        }

        io.dwg.core.io.ReadLimits.reserve((long) ((blockCount * ReedSolomonDecoder.CODEWORD_SIZE)), "R2007SystemPageReader");
        byte[] encoded = new byte[(int) (blockCount * ReedSolomonDecoder.CODEWORD_SIZE)];
        input.seek(fileOffset * 8);
        for (int i = 0; i < encoded.length && !input.isEof(); i++) {
            encoded[i] = (byte) input.readRawChar();
        }

        byte[] data = ReedSolomonDecoder.deinterleave(encoded, (int) blockCount,
            ReedSolomonDecoder.SYSTEM_DATA_SIZE);

        if (sizeComp < sizeUncomp) {
            return new Lz77Decompressor().decompress(data, (int) sizeUncomp);
        }
        io.dwg.core.io.ReadLimits.reserve((long) (sizeUncomp), "R2007SystemPageReader");
        byte[] page = new byte[(int) sizeUncomp];
        System.arraycopy(data, 0, page, 0, Math.min(page.length, data.length));
        return page;
    }

    /** Reads a little-endian 64-bit value at {@code offsetHolder[0]} and advances it by 8. */
    public static long readRLL(byte[] data, int[] offsetHolder) throws Exception {
        int offset = offsetHolder[0];
        if (offset + 8 > data.length) {
            throw new Exception("RLL read out of bounds");
        }
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value |= ((long) (data[offset + i] & 0xFF)) << (i * 8);
        }
        offsetHolder[0] += 8;
        return value;
    }
}
