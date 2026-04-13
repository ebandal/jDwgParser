package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * INSERT 엔티티 (타입 0x07)
 */
public class DwgInsert extends AbstractDwgEntity {
    private Point3D insertionPoint;
    private double xScale = 1.0;
    private double yScale = 1.0;
    private double zScale = 1.0;
    private double rotation;
    private double[] extrusion = {0.0, 0.0, 1.0};
    private boolean hasAttribs;
    private DwgHandleRef blockHeaderHandle;
    private DwgHandleRef firstAttribHandle;
    private DwgHandleRef lastAttribHandle;
    private DwgHandleRef seqendHandle;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.INSERT; }

    public Point3D insertionPoint() { return insertionPoint; }
    public double xScale() { return xScale; }
    public double yScale() { return yScale; }
    public double zScale() { return zScale; }
    public double rotation() { return rotation; }
    public double[] extrusion() { return extrusion; }
    public boolean hasAttribs() { return hasAttribs; }
    public DwgHandleRef blockHeaderHandle() { return blockHeaderHandle; }
    public DwgHandleRef firstAttribHandle() { return firstAttribHandle; }
    public DwgHandleRef lastAttribHandle() { return lastAttribHandle; }
    public DwgHandleRef seqendHandle() { return seqendHandle; }

    public void setInsertionPoint(Point3D p) { this.insertionPoint = p; }
    public void setXScale(double v) { this.xScale = v; }
    public void setYScale(double v) { this.yScale = v; }
    public void setZScale(double v) { this.zScale = v; }
    public void setRotation(double v) { this.rotation = v; }
    public void setExtrusion(double[] v) { this.extrusion = v; }
    public void setHasAttribs(boolean v) { this.hasAttribs = v; }
    public void setBlockHeaderHandle(DwgHandleRef h) { this.blockHeaderHandle = h; }
    public void setFirstAttribHandle(DwgHandleRef h) { this.firstAttribHandle = h; }
    public void setLastAttribHandle(DwgHandleRef h) { this.lastAttribHandle = h; }
    public void setSeqendHandle(DwgHandleRef h) { this.seqendHandle = h; }
}
