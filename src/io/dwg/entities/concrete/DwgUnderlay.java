package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * UNDERLAY 엔티티 - PDF/DWF 언더레이 (R2000+)
 */
public class DwgUnderlay extends AbstractDwgEntity {
    private String underlayPath;
    private Point3D insertionPoint;
    private double[] scale = {1.0, 1.0, 1.0};
    private double rotation;
    private int underlayType;  // 0=PDF, 1=DWF, 2=DGN
    private double opacity;
    private boolean isClipped;
    private int clipBoundaryType;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.UNDERLAY; }

    public String underlayPath() { return underlayPath; }
    public Point3D insertionPoint() { return insertionPoint; }
    public double[] scale() { return scale; }
    public double rotation() { return rotation; }
    public int underlayType() { return underlayType; }
    public double opacity() { return opacity; }
    public boolean isClipped() { return isClipped; }
    public int clipBoundaryType() { return clipBoundaryType; }

    public void setUnderlayPath(String underlayPath) { this.underlayPath = underlayPath; }
    public void setInsertionPoint(Point3D insertionPoint) { this.insertionPoint = insertionPoint; }
    public void setScale(double[] scale) { this.scale = scale; }
    public void setRotation(double rotation) { this.rotation = rotation; }
    public void setUnderlayType(int underlayType) { this.underlayType = underlayType; }
    public void setOpacity(double opacity) { this.opacity = opacity; }
    public void setIsClipped(boolean isClipped) { this.isClipped = isClipped; }
    public void setClipBoundaryType(int clipBoundaryType) { this.clipBoundaryType = clipBoundaryType; }
}
