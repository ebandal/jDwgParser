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
        int pageNum = 0;

        while (!input.isEof()) {
            // Per libredwg: last_handle and last_offset reset to 0 per page
            long lastHandle = 0;
            long lastOffset = 0;

            // Per libredwg: startpos is BEFORE reading section_size
            long blockStartBit = input.position();

            int byte1 = input.readRawChar() & 0xFF;
            if (input.isEof()) break;
            int byte2 = input.readRawChar() & 0xFF;
            int blockSize = (byte1 << 8) | byte2;  // RS_BE

            if (blockSize <= 2) {
                // Termination block: size=2 means no data, just CRC follows
                if (!input.isEof()) { input.readRawChar(); input.readRawChar(); } // skip CRC
                break;
            }

            if (blockSize > 2040) {
                System.out.printf("[DEBUG] HandlesParsingUtil: Invalid block size %d, stopping\n", blockSize);
                break;
            }

            // Per libredwg: data end = startpos + blockSize (blockSize includes the 2-byte header)
            long dataEndBit = blockStartBit + (long) blockSize * 8;

            int pairCount = 0;
            while (input.position() < dataEndBit && !input.isEof()) {
                int handleDelta = readUnsignedModularChar(input);
                if (handleDelta == 0) break;
                if (input.position() > dataEndBit) break;

                int offsetDelta = readSignedModularChar(input);

                lastHandle += handleDelta;
                lastOffset += offsetDelta;
                registry.put(lastHandle, lastOffset);
                pairCount++;
            }

            // Seek past data, read CRC (RS little-endian per libredwg bit_read_RS)
            input.seek(dataEndBit);
            if (!input.isEof()) {
                input.readRawShort(); // CRC (LE)
                if (pageNum == 0) System.out.printf("[DEBUG] HandlesParsingUtil: Block %d size=%d pairs=%d\n",
                    pageNum, blockSize, pairCount);
            }

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
        java.io.PrintWriter debugFile = null;
        try {
            debugFile = new java.io.PrintWriter(new java.io.FileWriter("handles_debug.txt"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        while (!reader.isEof()) {
            // Page size 읽기 (big-endian)
            long posBeforePageSize = reader.position();
            int pageSize = reader.readBigEndianShort();

            if (pageSize <= 2) {
                System.out.printf("[DEBUG] HandlesParsingUtil: Termination page (size=%d)\n", pageSize);
                break;
            }

            if (pageSize < 2 || pageSize > 2040) {
                System.out.printf("[DEBUG] HandlesParsingUtil: Invalid page size %d at bit pos %d (bytes: %d.%d), stopping\n",
                    pageSize, posBeforePageSize, posBeforePageSize / 8, posBeforePageSize % 8);
                break;
            }

            System.out.printf("[DEBUG] HandlesParsingUtil: Page %d size=%d\n", pageNum, pageSize);

            // Handle-offset 쌍 파싱 (pageSize - 2 bytes: only subtract size field, CRC is after pairs)
            // Page structure: [pageSize(2)] [pairs(pageSize-2)] [CRC(2)] [Total: pageSize+2]
            int pairsRead = 0;
            int bytesRead = 0;
            int pairsDataSize = pageSize - 2;  // pageSize includes 2-byte size field, pairs go from byte 2 to byte (pageSize-2)

            while (bytesRead < pairsDataSize && !reader.isEof()) {
                long beforePos = reader.position();

                // handle_delta: UMC (unsigned)
                int handleDelta = reader.readUnsignedModularChar();
                if (handleDelta == 0) break;

                // offset_delta: MC (signed)
                int offsetDelta = reader.readModularChar();

                long afterPos = reader.position();
                int bytesReadThisPair = (int)((afterPos - beforePos) / 8);
                bytesRead += bytesReadThisPair;

                lastHandle += handleDelta;
                lastOffset += offsetDelta;

                // Debug output to file for all pairs
                if (debugFile != null) {
                    debugFile.printf("pair[%d]: hDelta=0x%X cumH=0x%X oDelta=%d cumO=%d\n",
                        pairsRead, handleDelta, lastHandle, offsetDelta, lastOffset);
                    debugFile.flush();
                }

                // Debug output for first 20 pairs and every 50th pair
                if (pairsRead < 20 || pairsRead % 50 == 0) {
                    System.out.printf("[DEBUG] HandlesParsingUtil:   pair[%d]: hDelta=0x%X(→0x%X) oDelta=%d(→%d) (%db)\n",
                        pairsRead, handleDelta, lastHandle, offsetDelta, lastOffset, bytesReadThisPair);
                }

                registry.put(lastHandle, lastOffset);
                pairsRead++;
            }

            // R2007+ Handles: Align to byte boundary before reading CRC
            // Variable-length pair encoding may leave stream at non-byte-aligned position
            long currentPos = reader.position();
            if ((currentPos % 8) != 0) {
                // Skip padding bits to reach next byte boundary
                int paddingBits = 8 - (int)(currentPos % 8);
                reader.getInput().readBits(paddingBits);
                if (pairsRead < 10 || pairsRead % 50 == 0) {
                    System.out.printf("[DEBUG] HandlesParsingUtil: Byte-aligned from bit pos %d (skip %d bits)\n",
                        currentPos, paddingBits);
                }
            }

            // CRC 읽기 (big-endian, seed=0xC0C1)
            int crc = reader.readBigEndianShort();
            System.out.printf("[DEBUG] HandlesParsingUtil: Page %d: %d pairs, %d bytes, CRC=0x%04X\n",
                pageNum, pairsRead, bytesRead, crc);

            pageNum++;
        }

        System.out.printf("[DEBUG] HandlesParsingUtil: Parsed %d pages, total handles=%d\n",
            pageNum, registry.allHandles().size());

        if (debugFile != null) {
            debugFile.close();
        }
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
     * Per libredwg bits.c bit_read_MC():
     *   - Non-last bytes: 7 bits (0x7F mask) contribute to value
     *   - Last byte (high bit=0): bit 6 (0x40) is sign flag, cleared before use (0xBF mask)
     *     so only bits 0-5 contribute to the last byte's value
     */
    private static int readSignedModularChar(BitInput input) {
        int result = 0;
        int shift = 0;
        while (true) {
            int b = input.readRawChar() & 0xFF;
            if ((b & 0x80) != 0) {
                // Not the last byte: all 7 bits contribute
                result |= (b & 0x7F) << (shift * 7);
                shift++;
            } else {
                // Last byte: bit 6 is sign, clear it before computing value (per libredwg: byte &= 0xBF)
                boolean negative = (b & 0x40) != 0;
                b &= 0xBF;  // clear sign bit from value
                result |= (b & 0x7F) << (shift * 7);
                return negative ? -result : result;
            }
        }
    }

    /**
     * 누적 오프셋 계산 (모든 버전 동일)
     * Handles의 offset_delta는 누적되어야 함
     */
    public static long accumulateOffset(long currentOffset, int delta) {
        return currentOffset + delta;
    }
}
