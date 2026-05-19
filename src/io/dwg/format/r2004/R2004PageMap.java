package io.dwg.format.r2004;

import io.dwg.core.io.BitInput;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * R2004 페이지 맵. 페이지 ID → 파일 오프셋 매핑.
 * 스펙 §4.2: Page Map = pageId → offset 매핑 테이블.
 */
public class R2004PageMap {
    private final Map<Long, Long> pageOffsets = new HashMap<>();

    private R2004PageMap() {}

    /**
     * Page Map을 파일 오프셋에서 읽습니다.
     * 페이지 맵 구조 (스펙 §4.2):
     *   반복:
     *     pageId   (RL, 4 bytes) - 양수=데이터, 음수=여유 공간
     *     pageSize (RL, 4 bytes) - 페이지 크기
     *   파일 오프셋은 0x100에서 시작해 각 pageSize만큼 누적
     */
    public static R2004PageMap read(BitInput input, long pageMapOffset) throws Exception {
        R2004PageMap map = new R2004PageMap();

        long actualOffset = pageMapOffset * 8;
        input.seek(actualOffset);

        long runningOffset = 0x100;
        int entryCount = 0;

        while (entryCount < 10000) {
            long pageId = input.readRawLong() & 0xFFFFFFFFL;
            long pageSize = input.readRawLong() & 0xFFFFFFFFL;

            if (pageId == 0 && pageSize == 0) {
                break;
            }

            if (pageSize > 0x10000000) {
                break;
            }

            if (pageId > 0) {
                map.pageOffsets.put(pageId, runningOffset);
            }

            runningOffset += pageSize;
            entryCount++;
        }

        return map;
    }

    public Optional<Long> offsetForPage(long pageId) {
        return Optional.ofNullable(pageOffsets.get(pageId));
    }
}
