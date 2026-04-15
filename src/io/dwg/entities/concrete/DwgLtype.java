package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgObject;
import io.dwg.entities.DwgNonEntityObject;
import io.dwg.entities.DwgObjectType;

/**
 * LTYPE 테이블 엔트리 (타입 0x32)
 * 선 유형 정의 (실선, 점선, 대시 등)
 */
public class DwgLtype extends AbstractDwgObject implements DwgNonEntityObject {
    private String name = "";
    private String description = "";
    private double totalLength;
    private int numDashes;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.LTYPE; }

    @Override
    public boolean isEntity() { return false; }

    public String name() { return name; }
    public String description() { return description; }
    public double totalLength() { return totalLength; }
    public int numDashes() { return numDashes; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setTotalLength(double totalLength) { this.totalLength = totalLength; }
    public void setNumDashes(int numDashes) { this.numDashes = numDashes; }
}
