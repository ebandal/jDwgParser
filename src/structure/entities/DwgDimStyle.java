package structure.entities;

import structure.entities.AbstractDwgObject;
import structure.entities.DwgNonEntityObject;
import structure.entities.DwgObjectType;

/**
 * DIMSTYLE 테이블 엔트리 (타입 0x3A)
 * 치수 스타일 정의 (텍스트 크기, 화살표, 선 유형 등)
 */
public class DwgDimStyle extends AbstractDwgObject implements DwgNonEntityObject {
    private String name = "";
    private double textHeight = 1.0;
    private double textGap = 0.625;
    private double arrowSize = 1.0;
    private double lineExtension = 0.625;
    private double lineOffset = 0.625;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.DIMSTYLE; }

    @Override
    public boolean isEntity() { return false; }

    public String name() { return name; }
    public double textHeight() { return textHeight; }
    public double textGap() { return textGap; }
    public double arrowSize() { return arrowSize; }
    public double lineExtension() { return lineExtension; }
    public double lineOffset() { return lineOffset; }

    public void setName(String name) { this.name = name; }
    public void setTextHeight(double height) { this.textHeight = height; }
    public void setTextGap(double gap) { this.textGap = gap; }
    public void setArrowSize(double size) { this.arrowSize = size; }
    public void setLineExtension(double ext) { this.lineExtension = ext; }
    public void setLineOffset(double offset) { this.lineOffset = offset; }
}
