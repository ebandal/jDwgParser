package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * OLE2FRAME 엔티티 (타입 0x3E)
 * OLE 객체 임베딩 (Microsoft Word, Excel 등)
 */
public class DwgOle2frame extends AbstractDwgEntity {
    private Point3D insertionPoint;  // 삽입점
    private double[] scale;          // 스케일
    private double rotation;         // 회전각
    private String oleName;          // OLE 객체 이름
    private byte[] oleData;          // OLE 객체 데이터

    @Override
    public DwgObjectType objectType() { return DwgObjectType.OLE2FRAME; }

    public Point3D insertionPoint() { return insertionPoint; }
    public double[] scale() { return scale; }
    public double rotation() { return rotation; }
    public String oleName() { return oleName; }
    public byte[] oleData() { return oleData; }

    public void setInsertionPoint(Point3D insertionPoint) { this.insertionPoint = insertionPoint; }
    public void setScale(double[] scale) { this.scale = scale; }
    public void setRotation(double rotation) { this.rotation = rotation; }
    public void setOleName(String oleName) { this.oleName = oleName; }
    public void setOleData(byte[] oleData) { this.oleData = oleData; }
}
