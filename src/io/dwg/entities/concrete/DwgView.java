package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgObject;
import io.dwg.entities.DwgNonEntityObject;
import io.dwg.entities.DwgObjectType;

/**
 * VIEW 테이블 엔트리 (타입 0x36)
 * 저장된 뷰 정의 (카메라 위치, 줌 레벨 등)
 */
public class DwgView extends AbstractDwgObject implements DwgNonEntityObject {
    private String name = "";
    private Point3D viewTarget;
    private Point3D viewDirection;
    private double viewHeight = 1.0;
    private double viewWidth = 1.0;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.VIEW; }

    @Override
    public boolean isEntity() { return false; }

    public String name() { return name; }
    public Point3D viewTarget() { return viewTarget; }
    public Point3D viewDirection() { return viewDirection; }
    public double viewHeight() { return viewHeight; }
    public double viewWidth() { return viewWidth; }

    public void setName(String name) { this.name = name; }
    public void setViewTarget(Point3D target) { this.viewTarget = target; }
    public void setViewDirection(Point3D direction) { this.viewDirection = direction; }
    public void setViewHeight(double height) { this.viewHeight = height; }
    public void setViewWidth(double width) { this.viewWidth = width; }
}
