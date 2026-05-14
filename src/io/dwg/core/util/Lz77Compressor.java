package io.dwg.core.util;

/**
 * R2007+ 섹션 데이터 압축
 * Decompressor의 역방향
 */
public class Lz77Compressor {
    private static final int WINDOW_SIZE = 0x8000;  // 32KB 슬라이딩 윈도우
    private static final int MAX_MATCH_LENGTH = 0xFF + 15;
    private static final int MIN_MATCH_LENGTH = 2;

    /**
     * raw 데이터를 LZ77로 압축
     */
    public byte[] compress(byte[] raw) {
        io.dwg.core.io.ByteBufferBitOutput out = new io.dwg.core.io.ByteBufferBitOutput();
        int pos = 0;

        while (pos < raw.length) {
            int bestLength = 0;
            int bestOffset = 0;

            // 최장 매칭 탐색
            int matchInfo = findLongestMatch(raw, pos);
            bestLength = (matchInfo >> 16) & 0xFFFF;
            bestOffset = matchInfo & 0xFFFF;

            if (bestLength >= MIN_MATCH_LENGTH && pos + bestLength <= raw.length) {
                // 역참조 인코딩
                out.writeBit(false);
                out.writeBits(bestOffset, 15);
                
                int encodeLength = bestLength - 2;
                if (encodeLength < 16) {
                    out.writeBits(encodeLength, 4);
                } else {
                    out.writeBits(0, 4);
                    out.writeRawChar(Math.min(encodeLength - 15, 0xFF));
                }
                
                pos += bestLength;
            } else {
                // Literal 인코딩
                out.writeBit(true);
                
                int literalCount = 0;
                int literalStart = pos;
                while (pos < raw.length && literalCount < 0xFF + 14) {
                    int nextMatch = findLongestMatch(raw, pos);
                    int nextLength = (nextMatch >> 16) & 0xFFFF;
                    
                    if (nextLength >= MIN_MATCH_LENGTH) {
                        break;
                    }
                    literalCount++;
                    pos++;
                }

                if (literalCount < 16) {
                    out.writeBits(literalCount - 1, 4);
                } else {
                    out.writeBits(0, 4);
                    out.writeRawChar(Math.min(literalCount - 1 - 15, 0xFF));
                }

                for (int i = 0; i < literalCount; i++) {
                    out.writeRawChar(raw[literalStart + i] & 0xFF);
                }
            }
        }

        return out.toByteArray();
    }

    private int findLongestMatch(byte[] data, int pos) {
        int windowStart = Math.max(0, pos - WINDOW_SIZE);
        int bestLength = 0;
        int bestOffset = 0;

        if (pos > 0 && windowStart < pos) {
            for (int i = windowStart; i < pos; i++) {
                int length = 0;
                while (length < MAX_MATCH_LENGTH && 
                       (i + length) < pos &&
                       (pos + length) < data.length && 
                       data[i + length] == data[pos + length]) {
                    length++;
                }

                if (length >= MIN_MATCH_LENGTH && length > bestLength) {
                    bestLength = length;
                    bestOffset = pos - i;
                }
            }
        }

        // 상위 16비트: length, 하위 16비트: offset
        return ((bestLength & 0xFFFF) << 16) | (bestOffset & 0xFFFF);
    }
}
