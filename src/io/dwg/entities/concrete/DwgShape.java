package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * SHAPE 엔티티 (타입 0x21)
 * 형태 파일(.SHX)에 정의된 모양
 */
public class DwgShape extends AbstractDwgEntity {
    private Point3D insertionPoint;
    private String shapeName;
    private double scale;
    private double angle;
    private double[] extrusion = {0.0, 0.0, 1.0};

    @Override
    public DwgObjectType objectType() { return DwgObjectType.SHAPE; }

    public Point3D insertionPoint() { return insertionPoint; }
    public String shapeName() { return shapeName; }
    public double scale() { return scale; }
    public double angle() { return angle; }
    public double[] extrusion() { return extrusion; }

    public void setInsertionPoint(Point3D insertionPoint) { this.insertionPoint = insertionPoint; }
    public void setShapeName(String shapeName) { this.shapeName = shapeName; }
    public void setScale(double scale) { this.scale = scale; }
    public void setAngle(double angle) { this.angle = angle; }
    public void setExtrusion(double[] extrusion) { this.extrusion = extrusion; }
}
