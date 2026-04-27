package io.dwg.format.r2007;

import io.dwg.core.io.BitInput;
import io.dwg.core.util.ByteUtils;
import io.dwg.core.util.ReedSolomonDecoder;
import io.dwg.core.util.Lz77Decompressor;

/**
 * §5.2 R2007 파일 헤더 구조.
 *
 * 참고: R2007은 R2004의 XOR 복호화 대신 Reed-Solomon 에러정정코드를 사용합니다.
 * 파일 오프셋 0x80부터 0x3d8(952) 바이트를 읽어서 RS(255,239) 복호화를 수행해야 합니다.
 * 현재 구현은 미완성 상태입니다.
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
     *
     * R2007은 XOR 복호화 대신 RS(255,239) 에러정정코드를 사용하므로
     * 다음과 같은 단계를 따릅니다:
     * 1. 0x80부터 0x3d8(952) 바이트 읽기
     * 2. Reed-Solomon 복호화 (3개의 239-바이트 블록) → 717 바이트 복호화됨
     * 3. 복호화된 데이터 offset 32부터 Dwg_R2007_Header 필드 추출
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
            System.out.println("[DEBUG] RS decoder failed or returned insufficient data. Length: " +
                (decodedHeader == null ? "null" : decodedHeader.length));
            throw new Exception("RS decoding failed for R2007 header");
        }

        // Read metadata from decoded header (first 32 bytes)
        long comprLen = ByteUtils.readLE32(decodedHeader, 24) & 0xFFFFFFFFL;

        // Decompress the header if needed
        byte[] decompressedHeader;
        if (comprLen > 0) {
            try {
                int comprLenInt = (int)(comprLen & 0xFFFFFFFFL);
                byte[] compressed = new byte[comprLenInt];
                System.arraycopy(decodedHeader, 32, compressed, 0, comprLenInt);
                Lz77Decompressor lz77 = new Lz77Decompressor();
                decompressedHeader = lz77.decompress(compressed, 272);
            } catch (Exception e) {
                System.out.println("[DEBUG] LZ77 decompression failed: " + e.getMessage());
                throw new Exception("Failed to decompress R2007 header: " + e.getMessage());
            }
        } else {
            // Header is not compressed, use the raw decoded data
            decompressedHeader = decodedHeader;
        }

        // Extract fields from decompressed header (all LE64)
        // Dwg_R2007_Header structure:
        // +56: pages_map_offset
        // +80: pages_map_size_comp
        // +88: pages_map_size_uncomp
        // +144: sections_map_id
        // +128: sections_map_size_comp
        // +152: sections_map_size_uncomp

        if (decompressedHeader.length < 160) {
            System.out.println("[DEBUG] Decompressed header too small: " + decompressedHeader.length);
            throw new Exception("Decompressed R2007 header too small");
        }

        // Extract fields from decompressed header - all LE64
        // Per libredwg dwg.h struct r2007_file_header
        h.pageMapCorrection = ByteUtils.readLE64(decompressedHeader, 24);  // repeat_count for system page
        h.pageMapOffset = ByteUtils.readLE64(decompressedHeader, 56);
        h.pageMapSizeComp = ByteUtils.readLE64(decompressedHeader, 80);
        h.pageMapSizeUncomp = ByteUtils.readLE64(decompressedHeader, 88);
        h.sectionsMapSizeComp = ByteUtils.readLE64(decompressedHeader, 176);
        h.sectionsMapId = ByteUtils.readLE64(decompressedHeader, 192);
        h.sectionsMapSizeUncomp = ByteUtils.readLE64(decompressedHeader, 200);
        h.sectionsMapCorrection = ByteUtils.readLE64(decompressedHeader, 216);

        System.out.printf("[DEBUG] R2007 Header extracted: pageMapOffset=0x%X, comp=%d, uncomp=%d\n",
            h.pageMapOffset, h.pageMapSizeComp, h.pageMapSizeUncomp);

        return h;
    }

    /**
     * Reed-Solomon RS(255,239) 복호화
     * ReedSolomonDecoder를 사용하여 952바이트 데이터를 복호화합니다.
     *
     * 입력: 0x3d8(952) 바이트 (3개의 255-바이트 RS 블록)
     * 출력: 717 바이트 (3개의 239-바이트 데이터 블록)
     *
     * @param rsEncodedData RS 인코딩된 952 바이트
     * @return 복호화된 데이터 (또는 실패 시 null)
     */
    private static byte[] decodeReedSolomon(byte[] rsEncodedData) {
        if (rsEncodedData == null || rsEncodedData.length != 0x3d8) {
            return null;
        }

        // ReedSolomonDecoder를 사용하여 3개의 255-바이트 블록 복호화
        // (각 블록은 최대 8바이트의 에러를 수정할 수 있음)
        return ReedSolomonDecoder.decodeR2007Data(rsEncodedData);
    }

    @Deprecated
    private static byte[] xorDecrypt(byte[] data) {
        // R2004 스타일의 XOR 복호화 (R2007에서는 사용되지 않음)
        // R2007은 Reed-Solomon 에러정정코드를 사용합니다
        int[] key = new int[data.length];
        int seed = 0x4848;
        for (int i = 0; i < key.length; i++) {
            seed = (seed * 0x0343FD + 0x269EC3) & 0xFFFFFFFF;
            key[i] = (seed >> 16) & 0xFF;
        }
        byte[] out = new byte[data.length];
        for (int i = 0; i < data.length; i++) out[i] = (byte)(data[i] ^ key[i]);
        return out;
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
