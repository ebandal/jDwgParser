package io.dwg.entities.concrete;

import io.dwg.core.type.CmColor;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.entities.AbstractDwgTableRecord;
import io.dwg.entities.DwgObjectType;

/**
 * LAYER 테이블 엔트리 (타입 0x33)
 */
public class DwgLayer extends AbstractDwgTableRecord {
    private boolean isFrozen;
    private boolean isOn = true;
    private boolean isFrozenInNewViewports;
    private boolean isLocked;
    private int flags;
    private CmColor color;
    private DwgHandleRef lineTypeHandle;
    private DwgHandleRef plotStyleHandle;
    private double lineWeight;
    private boolean plotted = true;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.LAYER; }

    public boolean isFrozen() { return isFrozen; }
    public boolean isOn() { return isOn; }
    public boolean isFrozenInNewViewports() { return isFrozenInNewViewports; }
    public boolean isLocked() { return isLocked; }
    public int flags() { return flags; }
    public CmColor color() { return color; }
    public DwgHandleRef lineTypeHandle() { return lineTypeHandle; }
    public DwgHandleRef plotStyleHandle() { return plotStyleHandle; }
    /** R2000+: the lineweight, bits 0x03E0 of the layer flags shifted down by 5. */
    public double lineWeight() { return lineWeight; }
    /** R2000+: the layer is plotted (DXF 290). */
    public boolean isPlotted() { return plotted; }

    public void setFrozen(boolean frozen) { this.isFrozen = frozen; }
    public void setOn(boolean on) { this.isOn = on; }
    public void setFrozenInNewViewports(boolean v) { this.isFrozenInNewViewports = v; }
    public void setLocked(boolean locked) { this.isLocked = locked; }
    public void setFlags(int flags) { this.flags = flags; }
    public void setColor(CmColor color) { this.color = color; }
    public void setLineTypeHandle(DwgHandleRef h) { this.lineTypeHandle = h; }
    public void setPlotStyleHandle(DwgHandleRef h) { this.plotStyleHandle = h; }
    public void setLineWeight(double lineWeight) { this.lineWeight = lineWeight; }
    public void setPlotted(boolean plotted) { this.plotted = plotted; }
}
