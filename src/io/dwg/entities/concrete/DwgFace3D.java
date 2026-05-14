package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * 3DFACE 엔티티 (타입 0x1C)
 * 3D 공간의 면 (3개 또는 4개의 3D 점)
 */
public class DwgFace3D extends AbstractDwgEntity {
    private Point3D[] points = new Point3D[4];

    @Override
    public DwgObjectType objectType() { return DwgObjectType.FACE3D; }

    public Point3D[] points() { return points; }

    public void setPoints(Point3D[] points) {
        this.points = points;
    }

    public void setPoint(int index, Point3D point) {
        if (index >= 0 && index < 4) {
            this.points[index] = point;
        }
    }
}
