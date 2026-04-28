package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * XREF 엔티티 (외부 참조) - 다른 DWG 파일에 대한 참조
 */
public class DwgXref extends AbstractDwgEntity {
    private String referencePath;
    private Point3D insertionPoint;
    private double[] scale = {1.0, 1.0, 1.0};
    private double rotation;
    private int xrefType;
    private boolean isOverlaid;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.XREF; }

    public String referencePath() { return referencePath; }
    public Point3D insertionPoint() { return insertionPoint; }
    public double[] scale() { return scale; }
    public double rotation() { return rotation; }
    public int xrefType() { return xrefType; }
    public boolean isOverlaid() { return isOverlaid; }

    public void setReferencePath(String referencePath) { this.referencePath = referencePath; }
    public void setInsertionPoint(Point3D insertionPoint) { this.insertionPoint = insertionPoint; }
    public void setScale(double[] scale) { this.scale = scale; }
    public void setRotation(double rotation) { this.rotation = rotation; }
    public void setXrefType(int xrefType) { this.xrefType = xrefType; }
    public void setIsOverlaid(boolean isOverlaid) { this.isOverlaid = isOverlaid; }
}
