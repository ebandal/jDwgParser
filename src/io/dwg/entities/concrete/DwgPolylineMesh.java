package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * POLYLINE_MESH 엔티티 (타입 0x1E)
 * 메시 폴리라인 (M과 N 방향의 격자)
 * VERTEX_MESH 객체들의 컨테이너
 */
public class DwgPolylineMesh extends AbstractDwgEntity {
    private int flags;              // 폴리라인 플래그
    private int mVertexCount;       // M 방향 꼭지점 개수
    private int nVertexCount;       // N 방향 꼭지점 개수
    private int mDensity;           // M 방향 곡선 밀도
    private int nDensity;           // N 방향 곡선 밀도
    private double[] extrusion;     // 돌출 벡터

    @Override
    public DwgObjectType objectType() { return DwgObjectType.POLYLINE_MESH; }

    public int flags() { return flags; }
    public int mVertexCount() { return mVertexCount; }
    public int nVertexCount() { return nVertexCount; }
    public int mDensity() { return mDensity; }
    public int nDensity() { return nDensity; }
    public double[] extrusion() { return extrusion; }

    public void setFlags(int flags) { this.flags = flags; }
    public void setMVertexCount(int count) { this.mVertexCount = count; }
    public void setNVertexCount(int count) { this.nVertexCount = count; }
    public void setMDensity(int density) { this.mDensity = density; }
    public void setNDensity(int density) { this.nDensity = density; }
    public void setExtrusion(double[] extrusion) { this.extrusion = extrusion; }

    public boolean isClosed() { return (flags & 0x01) != 0; }
    public boolean isMClosed() { return (flags & 0x02) != 0; }
    public boolean isNClosed() { return (flags & 0x04) != 0; }
}
