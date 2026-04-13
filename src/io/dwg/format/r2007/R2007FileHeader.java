package io.dwg.format.r2007;

import io.dwg.core.io.BitInput;
import io.dwg.core.util.ReedSolomonDecoder;

/**
 * §5.2 R2007 파일 헤더 구조.
 *
 * 참고: R2007은 R2004의 XOR 복호화 대신 Reed-Solomon 에러정정코드를 사용합니다.
 * 파일 오프셋 0x80부터 0x3d8(952) 바이트를 읽어서 RS(255,239) 복호화를 수행해야 합니다.
 * 현재 구현은 미완성 상태입니다.
 */
public class R2007FileHeader {
    private long pageMapOffset;
    private long sectionMapId;

    private R2007FileHeader() {}

    /**
     * §5.2 헤더 파싱.
     *
     * 파일 구조:
     * - 0x00-0x05: 버전 문자열 (AC1021)
     * - 0x06-0x39: 라이브 데이터 필드 (0x34 = 52 바이트)
     * - 0x3A-0x7F: 패딩 (0x46 = 70 바이트)
     * - 0x80-0x3d7: Reed-Solomon 인코딩된 헤더 (0x358 = 856 바이트)
     *
     * R2007은 XOR 복호화 대신 RS(255,239) 에러정정코드를 사용하므로
     * 다음과 같은 단계를 따릅니다:
     * 1. 0x80부터 0x3d8(952) 바이트 읽기
     * 2. Reed-Solomon 복호화 (3개의 239-바이트 블록)
     * 3. 복호화된 데이터에서 필요한 필드 추출
     */
    public static R2007FileHeader read(BitInput input) throws Exception {
        R2007FileHeader h = new R2007FileHeader();

        // 0..5: 버전 문자열 (skip)
        for (int i = 0; i < 6; i++) input.readRawChar();

        // 6..10: 예약 (5바이트 skip)
        for (int i = 0; i < 5; i++) input.readRawChar();

        // 11: maintenance release
        input.readRawChar();

        // 12..15: byte 0 marker
        input.readRawChar();
        // 13..0x0D: preview offset (RL) – 예약 skip
        readLE32(input);
        // 17: app maintenance (skip)
        input.readRawChar();
        // 18: codepage (RS, skip)
        readLE16(input);

        // 20: securityFlags (RL, skip)
        readLE32(input);
        // 24: summary info offset (RL, skip)
        readLE32(input);
        // 28: vba project offset (RL, skip)
        readLE32(input);
        // 32: root tree node gap (RL, skip)
        readLE32(input);
        // 36: lower left gap (RL, skip)
        readLE32(input);
        // 40: lower right gap (RL, skip)
        readLE32(input);
        // 44: upper left gap (RL, skip)
        readLE32(input);
        // 48: upper right gap (RL, skip)
        readLE32(input);

        // §5.3 Reed-Solomon 인코딩 헤더 (파일 오프셋 0x80부터 시작)
        // 현재 위치가 0x3A (6 + 0x34)이므로 0x80까지 패딩을 스킵
        long currentBitPos = input.position();
        long currentBytePos = currentBitPos / 8;
        long paddingToSkip = 0x80 - currentBytePos;
        for (long i = 0; i < paddingToSkip; i++) {
            input.readRawChar();  // 패딩 스킵
        }

        // R2007은 0x3d8(952) 바이트의 Reed-Solomon 인코딩된 데이터를 사용
        // 이는 3개의 239-바이트 블록으로 RS(255,239) 인코딩됨
        byte[] rsEncodedData = new byte[0x3d8];
        for (int i = 0; i < rsEncodedData.length; i++) {
            rsEncodedData[i] = (byte) input.readRawChar();
        }

        // Reed-Solomon 복호화
        System.out.printf("[DEBUG] RS-encoded data: %d bytes\n", rsEncodedData.length);
        byte[] decodedData = decodeReedSolomon(rsEncodedData);

        if (decodedData == null) {
            System.err.println("[DEBUG] Reed-Solomon 복호화 실패 (null 반환)");
            // 임시로 빈 오프셋 사용
            h.pageMapOffset = 0;
            h.sectionMapId = 0;
            return h;
        }

        System.out.printf("[DEBUG] Decoded data: %d bytes\n", decodedData.length);
        if (decodedData.length < 32) {
            System.err.printf("[DEBUG] 데이터 부족: %d < 32\n", decodedData.length);
            h.pageMapOffset = 0;
            h.sectionMapId = 0;
            return h;
        }

        // 복호화된 데이터의 필드 추출
        // libredwg decode_r2007.c read_file_header() 참고
        // [0]:    seqence_crc64 (8 바이트) - 미사용
        // [8]:    seqence_key (8 바이트) - 페이지맵 오프셋으로 사용될 수 있음
        // [16]:   compr_crc64 (8 바이트) - 압축 데이터 CRC
        // [24]:   compr_len (4 바이트) - 압축 길이 (주로 사용됨)
        // [28]:   len2 (4 바이트) - 0 when compressed
        h.pageMapOffset = readLE64(decodedData, 0);     // seqence_crc64 as pageMapOffset (임시)
        h.sectionMapId = readLE64(decodedData, 16);     // compr_crc64 as sectionMapId (임시)

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
    public long sectionMapId()  { return sectionMapId; }

    // helpers
    private static long readLE32(BitInput in) {
        long v = 0;
        for (int i = 0; i < 4; i++) v |= ((long)(in.readRawChar() & 0xFF)) << (i * 8);
        return v;
    }
    private static int readLE16(BitInput in) {
        return (in.readRawChar() & 0xFF) | ((in.readRawChar() & 0xFF) << 8);
    }
    private static long readLE64(byte[] data, int off) {
        long v = 0;
        for (int i = 0; i < 8; i++) v |= ((long)(data[off + i] & 0xFF)) << (i * 8);
        return v;
    }
}
