package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * BLOCK 엔티티 (타입 0x04)
 * 블록 정의의 시작 마커. 블록의 속성은 BLOCK_HEADER 테이블 레코드({@link DwgBlockHeader})에 있다.
 */
public class DwgBlock extends AbstractDwgEntity {
    private String name;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.BLOCK; }

    public String name() { return name; }
    public void setName(String name) { this.name = name; }
}
