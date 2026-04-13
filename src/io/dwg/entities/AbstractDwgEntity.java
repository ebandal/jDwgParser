package io.dwg.entities;

import io.dwg.core.type.CmColor;
import io.dwg.core.type.DwgHandleRef;

/**
 * DwgEntity의 공통 필드 구현.
 */
public abstract class AbstractDwgEntity extends AbstractDwgObject implements DwgEntity {
    protected int entityMode;
    protected DwgHandleRef layerHandle;
    protected DwgHandleRef lineTypeHandle;
    protected double lineTypeScale = 1.0;
    protected CmColor color;
    protected int invisibility;
    protected double lineWeight;
    protected int plotStyleFlags;

    @Override public boolean isEntity() { return true; }
    @Override public int entityMode() { return entityMode; }
    @Override public DwgHandleRef layerHandle() { return layerHandle; }
    @Override public DwgHandleRef lineTypeHandle() { return lineTypeHandle; }
    @Override public double lineTypeScale() { return lineTypeScale; }
    @Override public CmColor color() { return color; }
    @Override public int invisibility() { return invisibility; }
    @Override public double lineWeight() { return lineWeight; }

    public void setEntityMode(int entityMode) { this.entityMode = entityMode; }
    public void setLayerHandle(DwgHandleRef layerHandle) { this.layerHandle = layerHandle; }
    public void setLineTypeHandle(DwgHandleRef lineTypeHandle) { this.lineTypeHandle = lineTypeHandle; }
    public void setLineTypeScale(double lineTypeScale) { this.lineTypeScale = lineTypeScale; }
    public void setColor(CmColor color) { this.color = color; }
    public void setInvisibility(int invisibility) { this.invisibility = invisibility; }
    public void setLineWeight(double lineWeight) { this.lineWeight = lineWeight; }
    public void setPlotStyleFlags(int plotStyleFlags) { this.plotStyleFlags = plotStyleFlags; }
}
