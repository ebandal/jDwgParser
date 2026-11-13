package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.entities.DwgDimension;

/**
 * DIMENSION_ORDINATE 엔티티 (타입 0x14)
 * 좌표 크기 지정 (X 또는 Y 좌표만 표시)
 */
public class DwgDimensionOrdinate extends AbstractDwgEntity implements DwgDimension {
    private DwgHandleRef dimStyleHandle;
    private DwgHandleRef blockHandle;      // 치수 그림이 들어 있는 익명 블록
    private Point3D definitionPoint;   // 정의점
    private Point3D midpointOfText;    // 텍스트 중점
    private String text;               // 텍스트 내용
    private double textRotation;       // 텍스트 회전각
    private double insertionScale;     // 스케일
    private String dimensionStyleName; // 크기 지정 스타일

    @Override
    public DwgObjectType objectType() { return DwgObjectType.DIMENSION_ORDINATE; }

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

    @Override public DwgHandleRef dimStyleHandle() { return dimStyleHandle; }
    @Override public DwgHandleRef blockHandle() { return blockHandle; }
    @Override public void setDimStyleHandle(DwgHandleRef dimStyleHandle) { this.dimStyleHandle = dimStyleHandle; }
    @Override public void setBlockHandle(DwgHandleRef blockHandle) { this.blockHandle = blockHandle; }
}
