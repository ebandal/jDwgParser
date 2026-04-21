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
     *
     * 형식:
     * - [RS_BE] block_size (= 페이지 크기, max 2040)
     * - [page_size - 4 bytes] handle-offset 델타 쌍
     * - [RS_BE] crc
     * - 반복, block_size <= 2일 때 종료
     */
    public static void parseHandlesBlocksR13(BitInput input, HandleRegistry registry) {
        long lastHandle = 0;
        long lastOffset = 0;
        int pageNum = 0;

        while (!input.isEof()) {
            // Block size 읽기 (big-endian)
            int byte1 = input.readRawChar() & 0xFF;
            int byte2 = input.readRawChar() & 0xFF;
            int blockSize = (byte1 << 8) | byte2;  // RS_BE

            if (blockSize <= 2) {
                System.out.printf("[DEBUG] HandlesParsingUtil: Termination block (size=%d)\n", blockSize);
                break;
            }

            if (blockSize < 2 || blockSize > 2040) {
                System.out.printf("[DEBUG] HandlesParsingUtil: Invalid block size %d, stopping\n", blockSize);
                break;
            }

            System.out.printf("[DEBUG] HandlesParsingUtil: Block %d size=%d\n", pageNum, blockSize);

            // Handle-offset 쌍 파싱 (blockSize - 4 bytes: 앞의 size 2바이트 + 뒤의 CRC 2바이트 제외)
            int pairsSize = blockSize - 4;
            int pairsRead = 0;

            while (pairsRead < pairsSize && !input.isEof()) {
                int handleDelta = readUnsignedModularChar(input);
                if (handleDelta == 0) break;

                int offsetDelta = readSignedModularChar(input);

                lastHandle += handleDelta;
                lastOffset += offsetDelta;

                if (pairsRead < 3) {
                    System.out.printf("[DEBUG] HandlesParsingUtil:   pair: handle=0x%X offset=0x%X\n",
                        lastHandle, lastOffset);
                }

                registry.put(lastHandle, lastOffset);
                pairsRead++;
            }

            // CRC 읽기 (big-endian)
            byte1 = input.readRawChar() & 0xFF;
            byte2 = input.readRawChar() & 0xFF;
            int crc = (byte1 << 8) | byte2;  // RS_BE
            System.out.printf("[DEBUG] HandlesParsingUtil: Block %d CRC=0x%04X\n", pageNum, crc);

            pageNum++;
        }

        System.out.printf("[DEBUG] HandlesParsingUtil: Parsed %d blocks, total handles=%d\n",
            pageNum, registry.allHandles().size());
    }

    /**
     * R2000+ Handles 섹션 파싱 (페이지 기반, LZ77 해제 후)
     *
     * 압축되지 않은 스트림에서 읽음:
     * - [RS_BE] page_size (max 2040)
     * - [page_size - 4 bytes] handle-offset 델타 쌍
     * - [RS_BE] crc
     * - 반복, page_size <= 2일 때 종료
     */
    public static void parseHandlesPagesR2000(BitStreamReader reader, HandleRegistry registry) {
        long lastHandle = 0;
        long lastOffset = 0;
        int pageNum = 0;

        while (!reader.isEof()) {
            // Page size 읽기 (big-endian)
            int pageSize = reader.readBigEndianShort();

            if (pageSize <= 2) {
                System.out.printf("[DEBUG] HandlesParsingUtil: Termination page (size=%d)\n", pageSize);
                break;
            }

            if (pageSize < 2 || pageSize > 2040) {
                System.out.printf("[DEBUG] HandlesParsingUtil: Invalid page size %d, stopping\n", pageSize);
                break;
            }

            System.out.printf("[DEBUG] HandlesParsingUtil: Page %d size=%d\n", pageNum, pageSize);

            // Handle-offset 쌍 파싱 (pageSize - 4 bytes)
            int pairsRead = 0;
            int bytesRead = 0;
            int pairsDataSize = pageSize - 4;  // Subtract 2 for page size, 2 for CRC

            while (bytesRead < pairsDataSize && !reader.isEof()) {
                long beforePos = reader.position();

                // handle_delta: UMC (unsigned)
                int handleDelta = reader.readUnsignedModularChar();
                if (handleDelta == 0) break;

                // offset_delta: MC (signed) - can be negative!
                int offsetDelta = reader.readModularChar();

                long afterPos = reader.position();
                int bytesReadThisPair = (int)((afterPos - beforePos) / 8);
                bytesRead += bytesReadThisPair;

                lastHandle += handleDelta;
                lastOffset += offsetDelta;

                if (pairsRead < 3) {
                    System.out.printf("[DEBUG] HandlesParsingUtil:   pair: handle=0x%X offset=0x%X (%d bytes)\n",
                        lastHandle, lastOffset, bytesReadThisPair);
                }

                registry.put(lastHandle, lastOffset);
                pairsRead++;
            }

            // CRC 읽기 (big-endian, seed=0xC0C1)
            int crc = reader.readBigEndianShort();
            System.out.printf("[DEBUG] HandlesParsingUtil: Page %d: %d pairs, %d bytes, CRC=0x%04X\n",
                pageNum, pairsRead, bytesRead, crc);

            pageNum++;
        }

        System.out.printf("[DEBUG] HandlesParsingUtil: Parsed %d pages, total handles=%d\n",
            pageNum, registry.allHandles().size());
    }

    /**
     * UMC (unsigned modular char) 읽기
     * MC 인코딩이지만 부호는 없음 (항상 양수)
     */
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
     * 음수 플래그는 마지막 바이트의 bit 6 (0x40)
     */
    private static int readSignedModularChar(BitInput input) {
        int result = 0;
        int shift = 0;
        int b;
        do {
            b = input.readRawChar() & 0xFF;
            result |= (b & 0x7F) << (shift * 7);
            shift++;
        } while ((b & 0x80) != 0);

        // 음수 플래그 처리 (0x40)
        if ((b & 0x40) != 0) {
            result = -result;
        }
        return result;
    }

    /**
     * 누적 오프셋 계산 (모든 버전 동일)
     * Handles의 offset_delta는 누적되어야 함
     */
    public static long accumulateOffset(long currentOffset, int delta) {
        return currentOffset + delta;
    }
}
