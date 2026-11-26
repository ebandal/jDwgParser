package io.dwg.entities;

import io.dwg.core.type.CmColor;
import io.dwg.core.type.DwgHandleRef;

/**
 * DwgEntity의 공통 필드 구현.
 * <p>The small codes are kept in bytes (entity mode 0-3, invisibility 0/1, plot style flags 0-3)
 * and the line weight, an 8-bit code in the file, in a float: every drawing has many entities.
 */
public abstract class AbstractDwgEntity extends AbstractDwgObject implements DwgEntity {
    protected byte entityMode;
    protected DwgHandleRef layerHandle;
    protected DwgHandleRef lineTypeHandle;
    protected double lineTypeScale = 1.0;
    protected CmColor color;
    protected byte invisibility;
    protected float lineWeight;
    protected byte plotStyleFlags;

    @Override public boolean isEntity() { return true; }
    @Override public int entityMode() { return entityMode; }
    @Override public DwgHandleRef layerHandle() { return layerHandle; }
    @Override public DwgHandleRef lineTypeHandle() { return lineTypeHandle; }
    @Override public double lineTypeScale() { return lineTypeScale; }
    @Override public CmColor color() { return color; }
    @Override public int invisibility() { return invisibility; }
    @Override public double lineWeight() { return lineWeight; }

    public void setEntityMode(int entityMode) { this.entityMode = (byte) entityMode; }
    public void setLayerHandle(DwgHandleRef layerHandle) { this.layerHandle = layerHandle; }
    public void setLineTypeHandle(DwgHandleRef lineTypeHandle) { this.lineTypeHandle = lineTypeHandle; }
    public void setLineTypeScale(double lineTypeScale) { this.lineTypeScale = lineTypeScale; }
    public void setColor(CmColor color) { this.color = color; }
    public void setInvisibility(int invisibility) { this.invisibility = (byte) invisibility; }
    public void setLineWeight(double lineWeight) { this.lineWeight = (float) lineWeight; }
    public void setPlotStyleFlags(int plotStyleFlags) { this.plotStyleFlags = (byte) plotStyleFlags; }
}
