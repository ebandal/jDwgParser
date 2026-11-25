package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

import java.util.ArrayList;
import java.util.List;

/**
 * MLINE 엔티티 (타입 0x2F)
 * 여러 개의 평행선 (MLSTYLE 참고)
 */
public class DwgMLine extends AbstractDwgEntity {
    private String styleHandle;
    private double scale;
    private int justification;  // 0=top, 1=middle, 2=bottom
    private List<Point3D> vertices = new ArrayList<>();
    private double[] extrusion = {0.0, 0.0, 1.0};

    @Override
    public DwgObjectType objectType() { return DwgObjectType.MLINE; }

    public String styleHandle() { return styleHandle; }
    public double scale() { return scale; }
    public int justification() { return justification; }
    public List<Point3D> vertices() { return vertices; }
    public double[] extrusion() { return extrusion; }

    public void setStyleHandle(String styleHandle) { this.styleHandle = styleHandle; }
    public void setScale(double scale) { this.scale = scale; }
    public void setJustification(int justification) { this.justification = justification; }
    public void addVertex(Point3D vertex) { this.vertices.add(vertex); }
    public void setExtrusion(double[] extrusion) { this.extrusion = extrusion; }

    /**
     * A vertex of the multiline: its point, direction and miter direction, and for each line of
     * the style the segment parameters and the area fill parameters.
     */
    public record Vertex(Point3D point, Point3D direction, Point3D miter,
                         List<double[]> segmentParameters, List<double[]> areaFillParameters) {
    }

    private final List<Vertex> vertexData = new ArrayList<>();
    private int openClosed;
    private io.dwg.core.type.DwgHandleRef mlineStyleHandle;

    public List<Vertex> vertexData() { return java.util.Collections.unmodifiableList(vertexData); }
    /** 1 open, 3 closed. */
    public int openClosed() { return openClosed; }
    /** The MLINESTYLE. */
    public io.dwg.core.type.DwgHandleRef mlineStyleHandle() { return mlineStyleHandle; }

    public void addVertexData(Vertex v) { vertexData.add(v); }
    public void setOpenClosed(int v) { this.openClosed = v; }
    public void setMlineStyleHandle(io.dwg.core.type.DwgHandleRef h) { this.mlineStyleHandle = h; }
}
