package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * POLYLINE_PFACE 엔티티 (타입 0x1D)
 * 면 폴리라인 (얼굴의 집합)
 * VERTEX_PFACE와 VERTEX_PFACE_FACE 객체들의 컨테이너
 */
public class DwgPolylinePface extends AbstractDwgEntity {
    private int numVertices;        // 꼭지점 개수
    private int numFaces;           // 면 개수
    private double[] extrusion;     // 돌출 벡터

    @Override
    public DwgObjectType objectType() { return DwgObjectType.POLYLINE_PFACE; }

    public int numVertices() { return numVertices; }
    public int numFaces() { return numFaces; }
    public double[] extrusion() { return extrusion; }

    public void setNumVertices(int count) { this.numVertices = count; }
    public void setNumFaces(int count) { this.numFaces = count; }
    public void setExtrusion(double[] extrusion) { this.extrusion = extrusion; }
}
