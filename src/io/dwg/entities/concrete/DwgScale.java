package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * SCALE 엔티티 - 축척 객체 (R2000+)
 */
public class DwgScale extends AbstractDwgEntity {
    private String scaleName;
    private double paperUnits;
    private double drawingUnits;
    private boolean isUnitScale;
    private boolean hasCustomScale;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.SCALE; }

    public String scaleName() { return scaleName; }
    public double paperUnits() { return paperUnits; }
    public double drawingUnits() { return drawingUnits; }
    public boolean isUnitScale() { return isUnitScale; }
    public boolean hasCustomScale() { return hasCustomScale; }

    public void setScaleName(String scaleName) { this.scaleName = scaleName; }
    public void setPaperUnits(double paperUnits) { this.paperUnits = paperUnits; }
    public void setDrawingUnits(double drawingUnits) { this.drawingUnits = drawingUnits; }
    public void setIsUnitScale(boolean isUnitScale) { this.isUnitScale = isUnitScale; }
    public void setHasCustomScale(boolean hasCustomScale) { this.hasCustomScale = hasCustomScale; }
}
