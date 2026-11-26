package io.dwg.format.r2007;

import io.dwg.core.io.BitInput;
import io.dwg.core.util.ByteUtils;
import io.dwg.core.util.Lz77Decompressor;
import io.dwg.core.util.ReedSolomonDecoder;

/**
 * The file header of R2007 files (spec §5.2).
 *
 * <p>After the 0x80-byte meta data comes a 0x400-byte block. Its first 0x3D8 bytes are three
 * interleaved Reed-Solomon code words; their data is:
 * <pre>
 *   0x00  8  CRC
 *   0x08  8  unknown key
 *   0x10  8  compressed data CRC
 *   0x18  4  ComprLen: positive = compressed length; negative = not compressed, length -ComprLen
 *   0x1C  4  length 2
 *   0x20     the data
 * </pre>
 * The data decompresses to 0x110 bytes of 64-bit fields, of which these are kept: page map
 * correction factor (0x18), page map offset (0x38), page map compressed and uncompressed size
 * (0x50, 0x58), section map compressed size (0xB0), section map id (0xC0), section map
 * uncompressed size (0xC8) and section map correction factor (0xD8).
 */
public class R2007FileHeader {

    private static final int META_DATA_SIZE = 0x80;
    private static final int ENCODED_SIZE = 0x3D8;
    private static final int DATA_OFFSET = 0x20;
    private static final int DECOMPRESSED_SIZE = 0x110;

    private static final int PAGE_MAP_CORRECTION = 0x18;
    private static final int PAGE_MAP_OFFSET = 0x38;
    private static final int PAGE_MAP_SIZE_COMP = 0x50;
    private static final int PAGE_MAP_SIZE_UNCOMP = 0x58;
    private static final int SECTIONS_MAP_SIZE_COMP = 0xB0;
    private static final int SECTIONS_MAP_ID = 0xC0;
    private static final int SECTIONS_MAP_SIZE_UNCOMP = 0xC8;
    private static final int SECTIONS_MAP_CORRECTION = 0xD8;
    private static final int FIELDS_END = SECTIONS_MAP_CORRECTION + 8;

    private long pageMapOffset;
    private long pageMapSizeComp;
    private long pageMapSizeUncomp;
    private long pageMapCorrection;
    private long sectionsMapId;
    private long sectionsMapSizeComp;
    private long sectionsMapSizeUncomp;
    private long sectionsMapCorrection;

    private R2007FileHeader() {
    }

    /** Reads the header from the start of the file. */
    public static R2007FileHeader read(BitInput input) throws Exception {
        skipBytes(input, META_DATA_SIZE);
        byte[] encoded = new byte[ENCODED_SIZE];
        for (int i = 0; i < encoded.length; i++) {
            encoded[i] = (byte) input.readRawChar();
        }

        byte[] decoded = ReedSolomonDecoder.decodeR2007Data(encoded);
        if (decoded == null || decoded.length < DATA_OFFSET) {
            throw new Exception("RS decoding failed for R2007 header");
        }

        int comprLen = (int) ByteUtils.readLE32(decoded, 0x18);
        int length = Math.abs(comprLen);
        int available = decoded.length - DATA_OFFSET;
        if (comprLen == Integer.MIN_VALUE || length > available) {
            throw new Exception("R2007 header: invalid comprLen=" + comprLen + " (available=" + available + ")");
        }

        byte[] fields;
        if (comprLen > 0) {
            io.dwg.core.io.ReadLimits.reserve((long) (length), "R2007FileHeader");
            byte[] compressed = new byte[length];
            System.arraycopy(decoded, DATA_OFFSET, compressed, 0, length);
            try {
                fields = new Lz77Decompressor().decompress(compressed, DECOMPRESSED_SIZE);
            } catch (io.dwg.core.io.ReadLimits.LimitExceededException limit) {
                throw limit;
            } catch (Exception e) {
                throw new Exception("Failed to decompress R2007 header: " + e.getMessage());
            }
        } else if (comprLen < 0) {
            io.dwg.core.io.ReadLimits.reserve((long) (length), "R2007FileHeader");
            fields = new byte[length];
            System.arraycopy(decoded, DATA_OFFSET, fields, 0, length);
        } else {
            fields = decoded;
        }
        if (fields.length < FIELDS_END) {
            throw new Exception("Decompressed R2007 header too small");
        }

        R2007FileHeader h = new R2007FileHeader();
        h.pageMapCorrection = ByteUtils.readLE64(fields, PAGE_MAP_CORRECTION);
        h.pageMapOffset = ByteUtils.readLE64(fields, PAGE_MAP_OFFSET);
        h.pageMapSizeComp = ByteUtils.readLE64(fields, PAGE_MAP_SIZE_COMP);
        h.pageMapSizeUncomp = ByteUtils.readLE64(fields, PAGE_MAP_SIZE_UNCOMP);
        h.sectionsMapSizeComp = ByteUtils.readLE64(fields, SECTIONS_MAP_SIZE_COMP);
        h.sectionsMapId = ByteUtils.readLE64(fields, SECTIONS_MAP_ID);
        h.sectionsMapSizeUncomp = ByteUtils.readLE64(fields, SECTIONS_MAP_SIZE_UNCOMP);
        h.sectionsMapCorrection = ByteUtils.readLE64(fields, SECTIONS_MAP_CORRECTION);
        return h;
    }

    private static void skipBytes(BitInput input, int count) {
        for (int i = 0; i < count; i++) {
            input.readRawChar();
        }
    }

    public long pageMapOffset() { return pageMapOffset; }
    public long pageMapCorrection() { return pageMapCorrection; }
    public long pageMapSizeComp() { return pageMapSizeComp; }
    public long pageMapSizeUncomp() { return pageMapSizeUncomp; }
    public long sectionsMapId() { return sectionsMapId; }
    public long sectionsMapSizeComp() { return sectionsMapSizeComp; }
    public long sectionsMapSizeUncomp() { return sectionsMapSizeUncomp; }
    public long sectionsMapCorrection() { return sectionsMapCorrection; }
}
