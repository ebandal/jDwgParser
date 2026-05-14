package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

import java.util.ArrayList;
import java.util.List;

/**
 * POLYLINE_3D 엔티티 (타입 0x10)
 * 3D 폴리라인 (여러 3D 선분의 연결)
 * VERTEX_3D 객체들의 컨테이너
 */
public class DwgPolyline3D extends AbstractDwgEntity {
    private List<Point3D> vertices = new ArrayList<>();
    private int flags;  // 닫힘, 면 플래그 등

    @Override
    public DwgObjectType objectType() { return DwgObjectType.POLYLINE_3D; }

    public List<Point3D> vertices() { return vertices; }
    public int flags() { return flags; }

    public void addVertex(Point3D vertex) { this.vertices.add(vertex); }
    public void setFlags(int flags) { this.flags = flags; }

    // 플래그 헬퍼 메서드
    public boolean isClosed() { return (flags & 0x01) != 0; }
    public boolean isMesh() { return (flags & 0x10) != 0; }
    public boolean isPolyfaceMesh() { return (flags & 0x20) != 0; }
}
