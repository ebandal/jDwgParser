package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

import java.util.ArrayList;
import java.util.List;

/**
 * POLYLINE_2D 엔티티 (타입 0x0F)
 * 2D 폴리라인 (여러 선분의 연결)
 * VERTEX_2D 객체들의 컨테이너
 */
public class DwgPolyline2D extends AbstractDwgEntity {
    private List<Point3D> vertices = new ArrayList<>();
    private int flags;  // 닫힘, 곡선 필 등의 플래그
    private double defaultStartWidth;
    private double defaultEndWidth;
    private double elevation;
    private double[] extrusion = {0.0, 0.0, 1.0};

    @Override
    public DwgObjectType objectType() { return DwgObjectType.POLYLINE_2D; }

    public List<Point3D> vertices() { return vertices; }
    public int flags() { return flags; }
    public double defaultStartWidth() { return defaultStartWidth; }
    public double defaultEndWidth() { return defaultEndWidth; }
    public double elevation() { return elevation; }
    public double[] extrusion() { return extrusion; }

    public void addVertex(Point3D vertex) { this.vertices.add(vertex); }
    public void setFlags(int flags) { this.flags = flags; }
    public void setDefaultStartWidth(double width) { this.defaultStartWidth = width; }
    public void setDefaultEndWidth(double width) { this.defaultEndWidth = width; }
    public void setElevation(double elevation) { this.elevation = elevation; }
    public void setExtrusion(double[] extrusion) { this.extrusion = extrusion; }

    // 플래그 헬퍼 메서드
    public boolean isClosed() { return (flags & 0x01) != 0; }
    public boolean isCurved() { return (flags & 0x04) != 0; }
}
