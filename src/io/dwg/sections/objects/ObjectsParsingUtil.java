package io.dwg.sections.objects;

/**
 * Objects 섹션 파싱의 공통 유틸리티.
 *
 * 핵심 규칙:
 * - 모든 DWG 버전(R13~R2018)에서 동일한 구조
 * - Objects는 offset 0부터 시작 (헤더 없음)
 * - Handles 섹션의 offset은 Objects 내 절대 바이트 위치
 * - offset_delta는 누적되어야 함 (offset[n] = offset[n-1] + delta[n])
 *
 * 주의: 실제 객체 파싱은 ObjectsSectionParser에서 수행
 */
public class ObjectsParsingUtil {

    /**
     * 누적 오프셋 계산
     * Objects 섹션의 오프셋은 누적되어야 함.
     * offset[n] = offset[n-1] + offset_delta[n]
     */
    public static long accumulateOffset(long currentOffset, int offsetDelta) {
        return currentOffset + offsetDelta;
    }

    /**
     * 검증: Objects 섹션이 유효한 위치에서 시작하는지 확인
     * - offset은 0부터 시작해야 함
     * - 첫 번째 객체는 offset 0에서 시작하는 경우도 있고,
     *   Handles 섹션에서 첫 번째 오프셋에서 시작하는 경우도 있음
     */
    public static boolean isValidObjectOffset(long offset, long sectionSize) {
        return offset >= 0 && offset < sectionSize;
    }

    /**
     * 객체 파싱 시작 위치 결정
     * - R13/R14: offset 0에서 시작
     * - R2000+: Handles 레지스트리가 있으면 그 위치에서 시작, 없으면 0에서 시작
     */
    public static long determineObjectsStartOffset(long firstHandleOffset, long minHandleOffset) {
        // 첫 번째 객체의 위치는 Handles 섹션의 오프셋으로 결정됨
        if (minHandleOffset > 0 && minHandleOffset < 512) {
            return minHandleOffset;
        }
        return 0;  // 기본값: offset 0부터 시작
    }
}
