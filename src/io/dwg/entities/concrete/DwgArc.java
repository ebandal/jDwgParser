package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * ARC 엔티티 (타입 0x11)
 */
public class DwgArc extends AbstractDwgEntity {
    private Point3D center;
    private double radius;
    private double startAngle;
    private double endAngle;
    private double thickness;
    private double[] extrusion = {0.0, 0.0, 1.0};

    @Override
    public DwgObjectType objectType() { return DwgObjectType.ARC; }

    public Point3D center() { return center; }
    public double radius() { return radius; }
    public double startAngle() { return startAngle; }
    public double endAngle() { return endAngle; }
    public double thickness() { return thickness; }
    public double[] extrusion() { return extrusion; }

    public void setCenter(Point3D center) { this.center = center; }
    public void setRadius(double radius) { this.radius = radius; }
    public void setStartAngle(double startAngle) { this.startAngle = startAngle; }
    public void setEndAngle(double endAngle) { this.endAngle = endAngle; }
    public void setThickness(double thickness) { this.thickness = thickness; }
    public void setExtrusion(double[] extrusion) { this.extrusion = extrusion; }
}
