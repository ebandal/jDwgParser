package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * OLE2FRAME (a class, spec §20.4.88): an embedded OLE object. The file holds its flags, mode and
 * the OLE data; the position of the frame is inside that data, which is not interpreted here.
 */
public class DwgOle2frame extends AbstractDwgEntity {
    private int flags;
    private int mode;
    private byte[] oleData;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.OLE2FRAME; }

    /** DXF 70 */
    public int flags() { return flags; }
    /** R2000+ */
    public int mode() { return mode; }
    /** The OLE data as stored. */
    public byte[] oleData() { return oleData; }

    /** The file holds no insertion point outside the OLE data; always {@code null}. */
    public Point3D insertionPoint() { return null; }
    /** Not stored outside the OLE data; always (1, 1, 1). */
    public double[] scale() { return new double[] {1, 1, 1}; }
    /** Not stored outside the OLE data; always 0. */
    public double rotation() { return 0; }
    /** Not stored; always "". */
    public String oleName() { return ""; }

    public void setFlags(int flags) { this.flags = flags; }
    public void setMode(int mode) { this.mode = mode; }
    public void setOleData(byte[] data) { this.oleData = data; }
}
