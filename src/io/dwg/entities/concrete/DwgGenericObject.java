package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgObject;
import io.dwg.entities.DwgObjectType;

/**
 * 전용 클래스가 없는 객체. 타입과 핸들만 가진다.
 * 테이블 제어 객체(BLOCK_CONTROL 등)와, Classes 섹션에 정의됐지만 이 라이브러리가 모르는 타입에 쓰인다.
 * 원래 타입 코드는 {@link #rawTypeCode()}에 있다.
 */
public class DwgGenericObject extends AbstractDwgObject {
    private final DwgObjectType type;

    public DwgGenericObject(DwgObjectType type) {
        this.type = type;
    }

    @Override
    public DwgObjectType objectType() { return type; }

    @Override
    public boolean isEntity() { return false; }
}
