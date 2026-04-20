package structure.entities;

import structure.entities.Point2D;
import structure.entities.AbstractDwgEntity;
import structure.entities.DwgObjectType;
import java.util.ArrayList;
import java.util.List;

/**
 * LWPOLYLINE 엔티티 (타입 0x4B, enum 상에선 LWPLINE)
 * 경량화된 폴리라인 (2D)
 */
public class DwgLwPolyline extends AbstractDwgEntity {
    private int flags;
    private double constantWidth;
    private double elevation;
    private double thickness;
    private double[] extrusion = {0.0, 0.0, 1.0};
    private List<Point2D> vertices = new ArrayList<>();
    private List<Double> bulges = new ArrayList<>();
    private List<double[]> widths = new ArrayList<>();

    @Override
    public DwgObjectType objectType() { return DwgObjectType.LWPLINE; }

    public int flags() { return flags; }
    public double constantWidth() { return constantWidth; }
    public double elevation() { return elevation; }
    public double thickness() { return thickness; }
    public double[] extrusion() { return extrusion; }
    public List<Point2D> vertices() { return vertices; }
    public List<Double> bulges() { return bulges; }
    public List<double[]> widths() { return widths; }

    public boolean isClosed() { return (flags & 0x01) != 0; }
    public int vertexCount() { return vertices.size(); }

    public void setFlags(int flags) { this.flags = flags; }
    public void setConstantWidth(double constantWidth) { this.constantWidth = constantWidth; }
    public void setElevation(double elevation) { this.elevation = elevation; }
    public void setThickness(double thickness) { this.thickness = thickness; }
    public void setExtrusion(double[] extrusion) { this.extrusion = extrusion; }
    public void setVertices(List<Point2D> vertices) { this.vertices = vertices; }
    public void setBulges(List<Double> bulges) { this.bulges = bulges; }
    public void setWidths(List<double[]> widths) { this.widths = widths; }
}
