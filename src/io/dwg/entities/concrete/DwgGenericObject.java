package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgObject;
import io.dwg.entities.DwgObjectType;

/**
 * 전용 클래스가 없는 객체. 테이블 제어 객체(BLOCK_CONTROL 등)와, Classes 섹션에 정의됐지만 이
 * 라이브러리가 모르는 타입에 쓰인다. 원래 타입 코드는 {@link #rawTypeCode()}에 있다.
 * <p>모르는 클래스({@link DwgObjectType#UNKNOWN})와 프록시 객체는 클래스 이름과 객체 데이터를
 * 그대로 보관하므로, 호출하는 쪽에서 직접 해석하거나 그대로 옮겨 쓸 수 있다.
 */
public class DwgGenericObject extends AbstractDwgObject {
    private final DwgObjectType type;
    private boolean entity;
    private String className;

    public DwgGenericObject(DwgObjectType type) {
        this.type = type;
    }

    @Override
    public DwgObjectType objectType() { return type; }

    /** Classes 섹션이 엔티티로 정의한 클래스이면 true */
    @Override
    public boolean isEntity() { return entity; }

    /** Classes 섹션의 DXF 이름 (예: "ACDBDETAILVIEWSTYLE"). 클래스가 아니면 null */
    public String className() { return className; }

    public void setEntity(boolean entity) { this.entity = entity; }
    public void setClassName(String className) { this.className = className; }
}
