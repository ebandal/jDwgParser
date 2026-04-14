package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * BLOCK_END 엔티티 (타입 0x31)
 * 블록 정의의 끝 마커
 */
public class DwgBlockEnd extends AbstractDwgEntity {
    // 블록 끝 마커는 추가 데이터가 없음

    @Override
    public DwgObjectType objectType() { return DwgObjectType.BLOCK_END; }
}
