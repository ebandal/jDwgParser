package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * RAY 엔티티 (타입 0x28)
 * 시작점과 방향으로 정의된 반무한 선
 */
public class DwgRay extends AbstractDwgEntity {
    private Point3D start;
    private Point3D direction;  // 단위 벡터

    @Override
    public DwgObjectType objectType() { return DwgObjectType.RAY; }

    public Point3D start() { return start; }
    public Point3D direction() { return direction; }

    public void setStart(Point3D start) { this.start = start; }
    public void setDirection(Point3D direction) { this.direction = direction; }
}
