package structure.entities;

import structure.entities.AbstractDwgObject;
import structure.entities.DwgNonEntityObject;
import structure.entities.DwgObjectType;

/**
 * APPID 테이블 엔트리 (타입 0x39)
 * 애플리케이션 식별자 (확장 데이터 소유권)
 */
public class DwgAppId extends AbstractDwgObject implements DwgNonEntityObject {
    private String name = "";
    private int flags;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.APPID; }

    @Override
    public boolean isEntity() { return false; }

    public String name() { return name; }
    public int flags() { return flags; }

    public void setName(String name) { this.name = name; }
    public void setFlags(int flags) { this.flags = flags; }
}
