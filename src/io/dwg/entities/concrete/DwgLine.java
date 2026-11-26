package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * LINE 엔티티 (타입 0x13)
 */
public class DwgLine extends AbstractDwgEntity {
    private Point3D start;
    private Point3D end;
    private double thickness;
    private double[] extrusion = io.dwg.entities.Extrusions.shared();

    @Override
    public DwgObjectType objectType() { return DwgObjectType.LINE; }

    public Point3D start() { return start; }
    public Point3D end() { return end; }
    public double thickness() { return thickness; }
    public double[] extrusion() { return io.dwg.entities.Extrusions.copy(extrusion); }

    public void setStart(Point3D start) { this.start = start; }
    public void setEnd(Point3D end) { this.end = end; }
    public void setThickness(double thickness) { this.thickness = thickness; }
    public void setExtrusion(double[] extrusion) { this.extrusion = io.dwg.entities.Extrusions.share(extrusion); }
}
