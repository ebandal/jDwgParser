package structure.entities;

import structure.entities.Point3D;
import structure.entities.AbstractDwgEntity;
import structure.entities.DwgObjectType;

/**
 * BLOCK_HEADER 엔티티 (타입 0x30)
 * 블록 정의의 시작 마커
 */
public class DwgBlockHeader extends AbstractDwgEntity {
    private String blockName;       // 블록 이름
    private int flags;              // 블록 플래그
    private Point3D basePoint;      // 블록의 기준점
    private String xrefPath;        // 외부 참조 경로 (XREF인 경우)

    @Override
    public DwgObjectType objectType() { return DwgObjectType.BLOCK_HEADER; }

    public String blockName() { return blockName; }
    public int flags() { return flags; }
    public Point3D basePoint() { return basePoint; }
    public String xrefPath() { return xrefPath; }

    public void setBlockName(String blockName) { this.blockName = blockName; }
    public void setFlags(int flags) { this.flags = flags; }
    public void setBasePoint(Point3D basePoint) { this.basePoint = basePoint; }
    public void setXrefPath(String xrefPath) { this.xrefPath = xrefPath; }

    public boolean isAnonymous() { return (flags & 0x01) != 0; }
    public boolean hasAttributes() { return (flags & 0x02) != 0; }
    public boolean isXref() { return (flags & 0x04) != 0; }
    public boolean isXrefOverlay() { return (flags & 0x08) != 0; }
}
