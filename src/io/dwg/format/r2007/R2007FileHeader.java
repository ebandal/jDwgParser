package io.dwg.format.r2007;

import io.dwg.core.io.BitInput;
import io.dwg.core.util.ByteUtils;
import io.dwg.core.util.ReedSolomonDecoder;
import io.dwg.core.util.Lz77Decompressor;

/**
 * §5.2 R2007 파일 헤더 구조.
 *
 * 파일 오프셋 0x80부터 0x3d8(952) 바이트를 읽어서 RS(255,239) 복호화를 수행합니다.
 */
public class R2007FileHeader {
    private long pageMapOffset;
    private long pageMapSizeComp;
    private long pageMapSizeUncomp;
    private long pageMapCorrection;
    private long sectionsMapId;
    private long sectionsMapSizeComp;
    private long sectionsMapSizeUncomp;
    private long sectionsMapCorrection;

    private R2007FileHeader() {}

    /**
     * §5.2 헤더 파싱.
     *
     * 파일 구조:
     * - 0x00-0x05: 버전 문자열 (AC1021)
     * - 0x06-0x39: 라이브 데이터 필드 (0x34 = 52 바이트)
     * - 0x3A-0x7F: 패딩 (0x46 = 70 바이트)
     * - 0x80-0x3d7: Reed-Solomon 인코딩된 헤더 (0x358 = 952 바이트, 3×255 블록)
     */
    public static R2007FileHeader read(BitInput input) throws Exception {
        R2007FileHeader h = new R2007FileHeader();

        // Skip unencrypted header (0x00-0x7F, 128 bytes)
        byte[] unencryptedHeader = new byte[0x80];
        for (int i = 0; i < 0x80; i++) {
            unencryptedHeader[i] = (byte) input.readRawChar();
        }

        // Read RS-encoded header (0x80-0x3d7, 952 bytes = 3×255)
        byte[] rsEncodedHeader = new byte[0x3d8];
        for (int i = 0; i < 0x3d8; i++) {
            rsEncodedHeader[i] = (byte) input.readRawChar();
        }

        // Decode using Reed-Solomon
        byte[] decodedHeader = ReedSolomonDecoder.decodeR2007Data(rsEncodedHeader);

        if (decodedHeader == null || decodedHeader.length < 32) {
            throw new Exception("RS decoding failed for R2007 header");
        }

        // libredwg reads compr_len as int32_t (signed) at offset 24
        int comprLen = (int) ByteUtils.readLE32(decodedHeader, 24);

        if (comprLen < 0 || comprLen > decodedHeader.length - 32) {
            throw new Exception("R2007 header: invalid comprLen=" + comprLen
                + " (available=" + (decodedHeader.length - 32) + ")");
        }

        byte[] decompressedHeader;
        if (comprLen > 0) {
            try {
                byte[] compressed = new byte[comprLen];
                System.arraycopy(decodedHeader, 32, compressed, 0, comprLen);
                Lz77Decompressor lz77 = new Lz77Decompressor();
                decompressedHeader = lz77.decompress(compressed, 272);
            } catch (Exception e) {
                throw new Exception("Failed to decompress R2007 header: " + e.getMessage());
            }
        } else {
            decompressedHeader = decodedHeader;
        }

        if (decompressedHeader.length < 160) {
            throw new Exception("Decompressed R2007 header too small");
        }

        // Extract fields from decompressed header - all LE64
        // Per libredwg dwg.h struct r2007_file_header
        h.pageMapCorrection = ByteUtils.readLE64(decompressedHeader, 24);
        h.pageMapOffset = ByteUtils.readLE64(decompressedHeader, 56);
        h.pageMapSizeComp = ByteUtils.readLE64(decompressedHeader, 80);
        h.pageMapSizeUncomp = ByteUtils.readLE64(decompressedHeader, 88);
        h.sectionsMapSizeComp = ByteUtils.readLE64(decompressedHeader, 176);
        h.sectionsMapId = ByteUtils.readLE64(decompressedHeader, 192);
        h.sectionsMapSizeUncomp = ByteUtils.readLE64(decompressedHeader, 200);
        h.sectionsMapCorrection = ByteUtils.readLE64(decompressedHeader, 216);

        return h;
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
