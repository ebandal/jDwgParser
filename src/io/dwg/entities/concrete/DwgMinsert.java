package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * MINSERT 엔티티 (타입 0x08)
 * 다중 삽입 (배열로 배치된 블록 참조)
 */
public class DwgMinsert extends AbstractDwgEntity {
    private String blockName;       // 블록명
    private Point3D insertionPoint; // 삽입점
    private double[] scale;         // 스케일 (X, Y, Z)
    private double rotation;        // 회전각
    private int rowCount;           // 행 개수
    private int columnCount;        // 열 개수
    private double rowSpacing;      // 행 간격
    private double columnSpacing;   // 열 간격
    private double[] extrusion;     // 돌출 벡터

    @Override
    public DwgObjectType objectType() { return DwgObjectType.MINSERT; }

    public String blockName() { return blockName; }
    public Point3D insertionPoint() { return insertionPoint; }
    public double[] scale() { return scale; }
    public double rotation() { return rotation; }
    public int rowCount() { return rowCount; }
    public int columnCount() { return columnCount; }
    public double rowSpacing() { return rowSpacing; }
    public double columnSpacing() { return columnSpacing; }
    public double[] extrusion() { return extrusion; }

    public void setBlockName(String blockName) { this.blockName = blockName; }
    public void setInsertionPoint(Point3D insertionPoint) { this.insertionPoint = insertionPoint; }
    public void setScale(double[] scale) { this.scale = scale; }
    public void setRotation(double rotation) { this.rotation = rotation; }
    public void setRowCount(int count) { this.rowCount = count; }
    public void setColumnCount(int count) { this.columnCount = count; }
    public void setRowSpacing(double spacing) { this.rowSpacing = spacing; }
    public void setColumnSpacing(double spacing) { this.columnSpacing = spacing; }
    public void setExtrusion(double[] extrusion) { this.extrusion = extrusion; }
}
