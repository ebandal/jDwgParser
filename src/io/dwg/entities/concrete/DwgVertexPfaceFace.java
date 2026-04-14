package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * VERTEX_PFACE_FACE 엔티티 (타입 0x0E)
 * 면 폴리라인의 면 (꼭지점 인덱스)
 */
public class DwgVertexPfaceFace extends AbstractDwgEntity {
    private int vertex1Index;  // 첫 번째 꼭지점 인덱스
    private int vertex2Index;  // 두 번째 꼭지점 인덱스
    private int vertex3Index;  // 세 번째 꼭지점 인덱스
    private int vertex4Index;  // 네 번째 꼭지점 인덱스 (0이면 삼각형)

    @Override
    public DwgObjectType objectType() { return DwgObjectType.VERTEX_PFACE_FACE; }

    public int vertex1Index() { return vertex1Index; }
    public int vertex2Index() { return vertex2Index; }
    public int vertex3Index() { return vertex3Index; }
    public int vertex4Index() { return vertex4Index; }

    public void setVertex1Index(int index) { this.vertex1Index = index; }
    public void setVertex2Index(int index) { this.vertex2Index = index; }
    public void setVertex3Index(int index) { this.vertex3Index = index; }
    public void setVertex4Index(int index) { this.vertex4Index = index; }
}
