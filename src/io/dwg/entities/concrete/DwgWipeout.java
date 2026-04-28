package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * WIPEOUT엔티티 (타입 0x52) - 이미지를 가리는 직사각형 객체
 */
public class DwgWipeout extends AbstractDwgEntity {
    private Point3D insertionPoint;
    private double[] uVector = {1.0, 0.0, 0.0};
    private double[] vVector = {0.0, 1.0, 0.0};
    private double width;
    private double height;
    private int clippingState;
    private int wipeoutImageType;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.WIPEOUT; }

    public Point3D insertionPoint() { return insertionPoint; }
    public double[] uVector() { return uVector; }
    public double[] vVector() { return vVector; }
    public double width() { return width; }
    public double height() { return height; }
    public int clippingState() { return clippingState; }
    public int wipeoutImageType() { return wipeoutImageType; }

    public void setInsertionPoint(Point3D insertionPoint) { this.insertionPoint = insertionPoint; }
    public void setUVector(double[] uVector) { this.uVector = uVector; }
    public void setVVector(double[] vVector) { this.vVector = vVector; }
    public void setWidth(double width) { this.width = width; }
    public void setHeight(double height) { this.height = height; }
    public void setClippingState(int clippingState) { this.clippingState = clippingState; }
    public void setWipeoutImageType(int wipeoutImageType) { this.wipeoutImageType = wipeoutImageType; }
}
