package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * DIMENSION_ALIGNED 엔티티 (타입 0x16)
 * 정렬된 크기 지정 (객체에 평행한 치수)
 */
public class DwgDimensionAligned extends AbstractDwgEntity {
    private Point3D definitionPoint;   // 정의점
    private Point3D midpointOfText;    // 텍스트 중점
    private String text;               // 텍스트 내용
    private double textRotation;       // 텍스트 회전각
    private double insertionScale;     // 스케일
    private String dimensionStyleName; // 크기 지정 스타일

    @Override
    public DwgObjectType objectType() { return DwgObjectType.DIMENSION_ALIGNED; }

    public Point3D definitionPoint() { return definitionPoint; }
    public Point3D midpointOfText() { return midpointOfText; }
    public String text() { return text; }
    public double textRotation() { return textRotation; }
    public double insertionScale() { return insertionScale; }
    public String dimensionStyleName() { return dimensionStyleName; }

    public void setDefinitionPoint(Point3D definitionPoint) { this.definitionPoint = definitionPoint; }
    public void setMidpointOfText(Point3D midpointOfText) { this.midpointOfText = midpointOfText; }
    public void setText(String text) { this.text = text; }
    public void setTextRotation(double textRotation) { this.textRotation = textRotation; }
    public void setInsertionScale(double insertionScale) { this.insertionScale = insertionScale; }
    public void setDimensionStyleName(String dimensionStyleName) { this.dimensionStyleName = dimensionStyleName; }
}
