package structure.entities;

import structure.entities.Point3D;
import structure.entities.AbstractDwgEntity;
import structure.entities.DwgObjectType;

/**
 * POINT 엔티티 (타입 0x1B)
 * 점 객체
 */
public class DwgPoint extends AbstractDwgEntity {
    private Point3D position;
    private double thickness;
    private double[] extrusion = {0.0, 0.0, 1.0};
    private double xAxisAngle;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.POINT; }

    public Point3D position() { return position; }
    public double thickness() { return thickness; }
    public double[] extrusion() { return extrusion; }
    public double xAxisAngle() { return xAxisAngle; }

    public void setPosition(Point3D position) { this.position = position; }
    public void setThickness(double thickness) { this.thickness = thickness; }
    public void setExtrusion(double[] extrusion) { this.extrusion = extrusion; }
    public void setXAxisAngle(double xAxisAngle) { this.xAxisAngle = xAxisAngle; }
}
