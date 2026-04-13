package io.dwg.format.r2007;

import io.dwg.core.io.BitInput;

/**
 * §5.2 R2007 파일 헤더 구조.
 */
public class R2007FileHeader {
    private long pageMapOffset;
    private long sectionMapId;

    private R2007FileHeader() {}

    /**
     * §5.2 헤더 파싱.
     * 0x70(112)바이트 고정. 첫 6바이트 = 버전 문자열, 나머지는 XOR 복호화 후 해석.
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

        // §5.3 body: 암호화된 헤더 (0x6C=108 바이트) 읽기
        byte[] encBody = new byte[0x6C];
        for (int i = 0; i < encBody.length; i++) encBody[i] = (byte) input.readRawChar();

        // XOR 복호화 (같은 magic 사용)
        byte[] body = xorDecrypt(encBody);

        // pageMapOffset: body 내 오프셋 0x28..0x2F (8바이트 LE)
        h.pageMapOffset = readLE64(body, 0x28);
        // sectionMapId : body 내 오프셋 0x20..0x27 (8바이트 LE)
        h.sectionMapId  = readLE64(body, 0x20);

        return h;
    }

    private static byte[] xorDecrypt(byte[] data) {
        // R2007 헤더 XOR 키 스케줄 (spec §5.2)
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
