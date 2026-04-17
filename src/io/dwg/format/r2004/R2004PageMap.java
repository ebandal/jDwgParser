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

        System.out.printf("[DEBUG] R2004PageMap.read: pageMapOffset=0x%X\n", pageMapOffset);

        // pageMapOffset is in bytes, convert to bits for BitInput
        long actualOffset = pageMapOffset * 8;
        input.seek(actualOffset);

        long runningOffset = 0x100; // 파일의 데이터 시작 오프셋
        int entryCount = 0;

        // Read page map entries until we hit invalid data
        // We don't know how many entries, so read until pageId <= 0 and we've accumulated reasonable data
        while (entryCount < 10000) { // Safety limit
            long pageId = input.readRawLong() & 0xFFFFFFFFL;
            long pageSize = input.readRawLong() & 0xFFFFFFFFL;

            // 유효한 데이터 검증
            if (pageId == 0 && pageSize == 0) {
                // End of page map marker
                break;
            }

            if (pageSize > 0x10000000) {
                // 페이지 크기가 비정상적으로 큼 - end of map
                break;
            }

            System.out.printf("[DEBUG] R2004PageMap: [%d] pageId=0x%X, offset=0x%X, size=0x%X\n",
                entryCount, pageId, runningOffset, pageSize);

            if (pageId > 0) {
                // 양수 pageId만 저장 (음수는 여유 공간)
                map.pageOffsets.put(pageId, runningOffset);
            }

            runningOffset += pageSize;
            entryCount++;
        }

        System.out.printf("[DEBUG] R2004PageMap: Total %d entries loaded\n", map.pageOffsets.size());
        return map;
    }

    public Optional<Long> offsetForPage(long pageId) {
        return Optional.ofNullable(pageOffsets.get(pageId));
    }
}
