package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * IMAGE엔티티 (타입 0x51) - 삽입된 래스터 이미지
 */
public class DwgImage extends AbstractDwgEntity {
    private Point3D insertionPoint;
    private double[] uVector = {1.0, 0.0, 0.0};
    private double[] vVector = {0.0, 1.0, 0.0};
    private double width;
    private double height;
    private int clippingState;
    private double brightness;
    private double contrast;
    private double fade;
    private String imagePath;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.IMAGE; }

    public Point3D insertionPoint() { return insertionPoint; }
    public double[] uVector() { return uVector; }
    public double[] vVector() { return vVector; }
    public double width() { return width; }
    public double height() { return height; }
    public int clippingState() { return clippingState; }
    public double brightness() { return brightness; }
    public double contrast() { return contrast; }
    public double fade() { return fade; }
    public String imagePath() { return imagePath; }

    public void setInsertionPoint(Point3D insertionPoint) { this.insertionPoint = insertionPoint; }
    public void setUVector(double[] uVector) { this.uVector = uVector; }
    public void setVVector(double[] vVector) { this.vVector = vVector; }
    public void setWidth(double width) { this.width = width; }
    public void setHeight(double height) { this.height = height; }
    public void setClippingState(int clippingState) { this.clippingState = clippingState; }
    public void setBrightness(double brightness) { this.brightness = brightness; }
    public void setContrast(double contrast) { this.contrast = contrast; }
    public void setFade(double fade) { this.fade = fade; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
}
