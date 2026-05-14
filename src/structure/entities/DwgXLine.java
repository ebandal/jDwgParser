package structure.entities;

import structure.entities.Point3D;
import structure.entities.AbstractDwgEntity;
import structure.entities.DwgObjectType;

/**
 * XLINE 엔티티 (타입 0x29)
 * 시작점과 방향으로 정의된 무한 선 (양쪽 방향)
 */
public class DwgXLine extends AbstractDwgEntity {
    private Point3D start;
    private Point3D direction;  // 단위 벡터

    @Override
    public DwgObjectType objectType() { return DwgObjectType.XLINE; }

    public Point3D start() { return start; }
    public Point3D direction() { return direction; }

    public void setStart(Point3D start) { this.start = start; }
    public void setDirection(Point3D direction) { this.direction = direction; }
}
