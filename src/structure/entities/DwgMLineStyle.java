package structure.entities;

import structure.entities.AbstractDwgObject;
import structure.entities.DwgNonEntityObject;
import structure.entities.DwgObjectType;

/**
 * MLINESTYLE 테이블 엔트리 (타입 0x3D)
 * 복선 스타일 정의
 */
public class DwgMLineStyle extends AbstractDwgObject implements DwgNonEntityObject {
    private String name = "";
    private String description = "";
    private int flags;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.MLINESTYLE; }

    @Override
    public boolean isEntity() { return false; }

    public String name() { return name; }
    public String description() { return description; }
    public int flags() { return flags; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setFlags(int flags) { this.flags = flags; }
}
