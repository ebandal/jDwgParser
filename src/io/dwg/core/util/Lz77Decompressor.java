package io.dwg.core.util;

import io.dwg.core.io.BitInput;

/**
 * R2007+ 섹션 데이터 압축 해제
 * 스펙 §5의 압축 알고리즘 구현
 */
public class Lz77Decompressor {
    private static final int WINDOW_SIZE = 0x8000;  // 32KB 슬라이딩 윈도우

    /**
     * 압축 데이터를 expectedSize 크기로 해제
     */
    public byte[] decompress(byte[] compressed, int expectedSize) throws Exception {
        byte[] output = new byte[expectedSize];
        BitInput in = new io.dwg.core.io.ByteBufferBitInput(compressed);
        int outPos = 0;

        while (outPos < expectedSize && !in.isEof()) {
            int literalFlag = in.readBit() ? 1 : 0;
            
            if (literalFlag == 1) {
                // 1비트 + literal length
                int length = in.readBits(4);
                if (length == 0) {
                    length = in.readRawChar() + 15;
                }
                length += 1;  // minimum 1
                
                handleLiteralRun(in, length, output, outPos);
                outPos += length;
            } else {
                // 0비트 + 값 압축 데이터
                int offset = in.readBits(15);
                int length = in.readBits(4);
                
                if (length == 0) {
                    length = in.readRawChar() + 15;
                }
                length += 2;  // minimum 2
                
                if (offset >= 0) {
                    handleBackRef(offset, length, output, outPos);
                    outPos += length;
                }
            }
        }

        return output;
    }

    private void handleLiteralRun(BitInput in, int count, byte[] out, int outPos) {
        for (int i = 0; i < count; i++) {
            if (outPos + i < out.length) {
                out[outPos + i] = (byte)in.readRawChar();
            }
        }
    }

    private void handleBackRef(int offset, int length, byte[] out, int outPos) {
        int windowStart = Math.max(0, outPos - WINDOW_SIZE);
        int refPos = outPos - offset;
        
        if (refPos < windowStart || refPos >= outPos) {
            return;  // 유효하지 않은 참조
        }

        for (int i = 0; i < length; i++) {
            if (outPos + i < out.length) {
                out[outPos + i] = out[refPos + i];
            }
        }
    }
}
