package io.dwg.entities.concrete;

import io.dwg.core.type.Point2D;
import io.dwg.entities.AbstractDwgObject;
import io.dwg.entities.DwgNonEntityObject;
import io.dwg.entities.DwgObjectType;

/**
 * LAYOUT 객체 (타입 0x50)
 * 페이지 레이아웃 정의 (용지 크기, 여백 등)
 */
public class DwgLayout extends AbstractDwgObject implements DwgNonEntityObject {
    private String name = "";
    private int tabOrder;
    private Point2D paperSize;
    private double marginLeft;
    private double marginRight;
    private double marginTop;
    private double marginBottom;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.LAYOUT; }

    @Override
    public boolean isEntity() { return false; }

    public String name() { return name; }
    public int tabOrder() { return tabOrder; }
    public Point2D paperSize() { return paperSize; }
    public double marginLeft() { return marginLeft; }
    public double marginRight() { return marginRight; }
    public double marginTop() { return marginTop; }
    public double marginBottom() { return marginBottom; }

    public void setName(String name) { this.name = name; }
    public void setTabOrder(int order) { this.tabOrder = order; }
    public void setPaperSize(Point2D size) { this.paperSize = size; }
    public void setMarginLeft(double margin) { this.marginLeft = margin; }
    public void setMarginRight(double margin) { this.marginRight = margin; }
    public void setMarginTop(double margin) { this.marginTop = margin; }
    public void setMarginBottom(double margin) { this.marginBottom = margin; }
}
