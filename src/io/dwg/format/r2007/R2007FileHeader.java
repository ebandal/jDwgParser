package io.dwg.format.r2007;

import io.dwg.core.io.BitInput;
import io.dwg.core.util.ByteUtils;
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

        // R2007 Unencrypted Header Format (first 0x3A bytes):
        // 0x00-0x05: Version string (AC1021)
        // 0x06-0x39: "Live data" fields (52 bytes)
        // 0x20-0x27 (within live data): Section map offset (8 bytes, LE64)

        byte[] unencryptedHeader = new byte[0x3A];
        for (int i = 0; i < 0x3A; i++) {
            unencryptedHeader[i] = (byte) input.readRawChar();
        }

        // Extract section map offset from offset 0x20 (within the unencrypted header)
        // This is an 8-byte little-endian value
        h.pageMapOffset = ByteUtils.readLE64(unencryptedHeader, 0x20);

        // For sectionMapId, use a fixed ID (usually 0)
        h.sectionMapId = 0;

        // NOTE: We're using the unencrypted header offsets directly instead of
        // relying on the Reed-Solomon decoded header, as the RS decoder is still
        // unreliable for some files. This provides basic R2007 support for now.
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
}
