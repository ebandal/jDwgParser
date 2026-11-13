package io.dwg.sections.objects;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;

/**
 * 개별 객체 타입의 추가 데이터 파싱 계약.
 */
public interface ObjectReader {
    void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception;
    int objectType();

    /**
     * 타입 코드 다음에 오는 공통 헤더(bitsize, 핸들, EED 등)를 이 리더가 직접 읽는지 여부.
     * false이면 파서가 {@link EntityHeaderReader#readObjectHeader}로 비엔티티 공통 헤더를 먼저 읽고
     * 그 다음 위치에서 {@link #read}를 호출한다.
     */
    default boolean readsOwnHeader() { return true; }

    /**
     * 핸들 스트림에서 이 타입 고유의 핸들을 읽는다.
     * 파서가 공통 핸들(소유자, 리액터, 확장 사전, 엔티티의 레이어 등)을 읽은 다음 위치에서 호출한다.
     * 상대 핸들은 {@link BitStreamReader#readHandleReference(long)}에 {@code target.handle()}을 넘겨 푼다.
     */
    default void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception { }
}
