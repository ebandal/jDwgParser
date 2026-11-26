package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * CIRCLE 엔티티 (타입 0x12)
 */
public class DwgCircle extends AbstractDwgEntity {
    private Point3D center;
    private double radius;
    private double thickness;
    private double[] extrusion = io.dwg.entities.Extrusions.shared();

    @Override
    public DwgObjectType objectType() { return DwgObjectType.CIRCLE; }

    public Point3D center() { return center; }
    public double radius() { return radius; }
    public double thickness() { return thickness; }
    public double[] extrusion() { return io.dwg.entities.Extrusions.copy(extrusion); }

    public void setCenter(Point3D center) { this.center = center; }
    public void setRadius(double radius) { this.radius = radius; }
    public void setThickness(double thickness) { this.thickness = thickness; }
    public void setExtrusion(double[] extrusion) { this.extrusion = io.dwg.entities.Extrusions.share(extrusion); }
}
