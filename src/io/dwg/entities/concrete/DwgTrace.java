package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * TRACE 엔티티 (타입 0x20)
 * SOLID와 유사하지만 4개의 점으로만 정의됨
 */
public class DwgTrace extends AbstractDwgEntity {
    private Point3D[] points = new Point3D[4];
    private double[] extrusion = {0.0, 0.0, 1.0};

    @Override
    public DwgObjectType objectType() { return DwgObjectType.TRACE; }

    public Point3D[] points() { return points; }
    public double[] extrusion() { return extrusion; }

    public void setPoints(Point3D[] points) {
        this.points = points;
    }

    public void setPoint(int index, Point3D point) {
        if (index >= 0 && index < 4) {
            this.points[index] = point;
        }
    }

    public void setExtrusion(double[] extrusion) {
        this.extrusion = extrusion;
    }
}
