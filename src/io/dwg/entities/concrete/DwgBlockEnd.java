package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgObject;
import io.dwg.entities.DwgObjectType;

/**
 * ENDBLK 엔티티 (타입 0x05)
 * 블록 정의의 끝 마커
 */
public class DwgBlockEnd extends AbstractDwgObject {
    // 블록 끝 마커는 추가 데이터가 없음

    @Override
    public DwgObjectType objectType() { return DwgObjectType.ENDBLK; }

    @Override
    public boolean isEntity() { return false; }
}
