package structure.entities;

import structure.entities.Point3D;
import structure.entities.AbstractDwgObject;
import structure.entities.DwgNonEntityObject;
import structure.entities.DwgObjectType;

/**
 * UCS 테이블 엔트리 (타입 0x37)
 * 사용자 좌표계 정의
 */
public class DwgUcs extends AbstractDwgObject implements DwgNonEntityObject {
    private String name = "";
    private Point3D origin;
    private Point3D xDirection;
    private Point3D yDirection;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.UCS; }

    @Override
    public boolean isEntity() { return false; }

    public String name() { return name; }
    public Point3D origin() { return origin; }
    public Point3D xDirection() { return xDirection; }
    public Point3D yDirection() { return yDirection; }

    public void setName(String name) { this.name = name; }
    public void setOrigin(Point3D origin) { this.origin = origin; }
    public void setXDirection(Point3D xDir) { this.xDirection = xDir; }
    public void setYDirection(Point3D yDir) { this.yDirection = yDir; }
}
