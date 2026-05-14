package structure.entities;

import structure.entities.Point3D;
import structure.entities.AbstractDwgEntity;
import structure.entities.DwgObjectType;

/**
 * VERTEX_PFACE 엔티티 (타입 0x0D)
 * 면 폴리라인의 꼭지점 (좌표 및 플래그만 포함)
 */
public class DwgVertexPface extends AbstractDwgEntity {
    private Point3D location;
    private int flags;  // 꼭지점 플래그

    @Override
    public DwgObjectType objectType() { return DwgObjectType.VERTEX_PFACE; }

    public Point3D location() { return location; }
    public int flags() { return flags; }

    public void setLocation(Point3D location) { this.location = location; }
    public void setFlags(int flags) { this.flags = flags; }
}
