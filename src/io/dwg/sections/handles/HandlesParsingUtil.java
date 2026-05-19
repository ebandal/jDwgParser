package io.dwg.sections.handles;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitStreamReader;

/**
 * Handles 섹션 파싱의 공통 유틸리티.
 *
 * 핵심 규칙:
 * - 모든 DWG 버전(R13~R2018)에서 동일한 구조
 * - page_size와 CRC는 RS_BE (big-endian)
 * - handle_delta는 UMC (unsigned modular char)
 * - offset_delta는 MC (signed modular char) - 음수 가능
 * - 각 페이지는 독립적인 CRC 검증
 */
public class HandlesParsingUtil {

    /**
     * R13/R14 Handles 섹션 파싱 (블록 기반)
     */
    public static void parseHandlesBlocksR13(BitInput input, HandleRegistry registry) {
        while (!input.isEof()) {
            long lastHandle = 0;
            long lastOffset = 0;

            long blockStartBit = input.position();

            int byte1 = input.readRawChar() & 0xFF;
            if (input.isEof()) break;
            int byte2 = input.readRawChar() & 0xFF;
            int blockSize = (byte1 << 8) | byte2;  // RS_BE

            if (blockSize <= 2) {
                if (!input.isEof()) { input.readRawChar(); input.readRawChar(); }
                break;
            }

            if (blockSize > 2040) {
                break;
            }

            long dataEndBit = blockStartBit + (long) blockSize * 8;

            while (input.position() < dataEndBit && !input.isEof()) {
                int handleDelta = readUnsignedModularChar(input);
                if (handleDelta == 0) break;
                if (input.position() > dataEndBit) break;

                int offsetDelta = readSignedModularChar(input);

                lastHandle += handleDelta;
                lastOffset += offsetDelta;
                registry.put(lastHandle, lastOffset);
            }

            input.seek(dataEndBit);
            if (!input.isEof()) {
                input.readRawShort(); // CRC (LE)
            }
        }
    }

    /**
     * R2000+ Handles 섹션 파싱 (페이지 기반, LZ77 해제 후)
     */
    public static void parseHandlesPagesR2000(BitStreamReader reader, HandleRegistry registry) {
        long lastHandle = 0;
        long lastOffset = 0;

        while (!reader.isEof()) {
            int pageSize = reader.readBigEndianShort();

            if (pageSize <= 2) {
                break;
            }

            if (pageSize < 2 || pageSize > 2040) {
                break;
            }

            int bytesRead = 0;
            int pairsDataSize = pageSize - 2;

            while (bytesRead < pairsDataSize && !reader.isEof()) {
                long beforePos = reader.position();

                int handleDelta = reader.readUnsignedModularChar();
                if (handleDelta == 0) break;

                int offsetDelta = reader.readModularChar();

                long afterPos = reader.position();
                bytesRead += (int)((afterPos - beforePos) / 8);

                lastHandle += handleDelta;
                lastOffset += offsetDelta;

                registry.put(lastHandle, lastOffset);
            }

            long currentPos = reader.position();
            if ((currentPos % 8) != 0) {
                int paddingBits = 8 - (int)(currentPos % 8);
                reader.getInput().readBits(paddingBits);
            }

            reader.readBigEndianShort();
        }
    }

    private static int readUnsignedModularChar(BitInput input) {
        int result = 0;
        int shift = 0;
        int b;
        do {
            b = input.readRawChar() & 0xFF;
            result |= (b & 0x7F) << (shift * 7);
            shift++;
        } while ((b & 0x80) != 0);
        return result;
    }

    /**
     * MC (signed modular char) 읽기
     * Per libredwg bits.c bit_read_MC():
     *   - Non-last bytes: 7 bits (0x7F mask) contribute to value
     *   - Last byte (high bit=0): bit 6 (0x40) is sign flag, cleared before use (0xBF mask)
     */
    private static int readSignedModularChar(BitInput input) {
        int result = 0;
        int shift = 0;
        while (true) {
            int b = input.readRawChar() & 0xFF;
            if ((b & 0x80) != 0) {
                result |= (b & 0x7F) << (shift * 7);
                shift++;
            } else {
                boolean negative = (b & 0x40) != 0;
                b &= 0xBF;
                result |= (b & 0x7F) << (shift * 7);
                return negative ? -result : result;
            }
        }
    }

    public static long accumulateOffset(long currentOffset, int delta) {
        return currentOffset + delta;
    }
}
