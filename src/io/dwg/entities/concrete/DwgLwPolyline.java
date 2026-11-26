package io.dwg.entities.concrete;

import io.dwg.core.type.Point2D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;
import java.util.ArrayList;
import java.util.List;

/**
 * LWPOLYLINE 엔티티 (타입 0x4D, enum 상에선 LWPLINE)
 * 경량화된 폴리라인 (2D)
 */
public class DwgLwPolyline extends AbstractDwgEntity {
    private int flags;
    private double constantWidth;
    private double elevation;
    private double thickness;
    private double[] extrusion = io.dwg.entities.Extrusions.shared();
    private List<Point2D> vertices = new ArrayList<>();
    private List<Double> bulges = new ArrayList<>();
    private List<double[]> widths = new ArrayList<>();

    @Override
    public DwgObjectType objectType() { return DwgObjectType.LWPLINE; }

    public int flags() { return flags; }
    public double constantWidth() { return constantWidth; }
    public double elevation() { return elevation; }
    public double thickness() { return thickness; }
    public double[] extrusion() { return io.dwg.entities.Extrusions.copy(extrusion); }
    public List<Point2D> vertices() { return vertices; }
    public List<Double> bulges() { return bulges; }
    public List<double[]> widths() { return widths; }

    public boolean isClosed() { return (flags & 0x200) != 0; }
    public int vertexCount() { return vertices.size(); }

    public void setFlags(int flags) { this.flags = flags; }
    public void setConstantWidth(double constantWidth) { this.constantWidth = constantWidth; }
    public void setElevation(double elevation) { this.elevation = elevation; }
    public void setThickness(double thickness) { this.thickness = thickness; }
    public void setExtrusion(double[] extrusion) { this.extrusion = io.dwg.entities.Extrusions.share(extrusion); }
    public void setVertices(List<Point2D> vertices) { this.vertices = vertices; }
    public void setBulges(List<Double> bulges) { this.bulges = bulges; }
    public void setWidths(List<double[]> widths) { this.widths = widths; }
}
