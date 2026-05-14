package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * VIEWPORT 엔티티 (타입 0x22)
 * 레이아웃의 뷰포트 (페이퍼스페이스 뷰잉 윈도우)
 */
public class DwgViewport extends AbstractDwgEntity {
    private Point3D center;
    private double width;
    private double height;
    private double viewHeight;
    private double[] extrusion = {0.0, 0.0, 1.0};

    @Override
    public DwgObjectType objectType() { return DwgObjectType.VIEWPORT; }

    public Point3D center() { return center; }
    public double width() { return width; }
    public double height() { return height; }
    public double viewHeight() { return viewHeight; }
    public double[] extrusion() { return extrusion; }

    public void setCenter(Point3D center) { this.center = center; }
    public void setWidth(double width) { this.width = width; }
    public void setHeight(double height) { this.height = height; }
    public void setViewHeight(double viewHeight) { this.viewHeight = viewHeight; }
    public void setExtrusion(double[] extrusion) { this.extrusion = extrusion; }
}
