package io.dwg.entities;

import io.dwg.core.type.DwgHandleRef;

/**
 * A table record (LAYER, LTYPE, STYLE, DIMSTYLE, APPID, BLOCK_HEADER, ...): an object with a
 * name and the xref data every record has.
 *
 * <p>Spec §20.4.54 and the other table records: after the name come the 64-flag (B, the 64 bit
 * of DXF group 70), the xref index plus one (BS) and the xref-dependent flag (B); the handle
 * stream holds the handle of the xref block the record came from. R2007+ files keep only one BS,
 * see {@link io.dwg.sections.objects.EntityHeaderReader#readTableRecordFlags}.
 */
public abstract class AbstractDwgTableRecord extends AbstractDwgObject implements DwgNonEntityObject {
    private String name = "";
    private boolean referenced;
    private int xrefIndex = -1;
    private boolean xrefDependent;
    private DwgHandleRef xrefBlockHandle;

    @Override
    public boolean isEntity() { return false; }

    /** The record's name (DXF group 2). */
    public String name() { return name; }

    /**
     * The 64-flag: the record was referenced by an entity when the file was last edited. R2007+
     * files do not hold it; it is false there.
     */
    public boolean isReferenced() { return referenced; }

    /**
     * The index of the block header of the xref this record came from, or -1 when it does not
     * come from an xref. R2007+ files do not hold it; it is -1 there.
     */
    public int xrefIndex() { return xrefIndex; }

    /** The record depends on an xref (the 16 bit of DXF group 70). */
    public boolean isXrefDependent() { return xrefDependent; }

    /** The block of the xref this record came from; a null handle when there is none. */
    public DwgHandleRef xrefBlockHandle() { return xrefBlockHandle; }

    public void setName(String name) { this.name = name == null ? "" : name; }
    public void setReferenced(boolean referenced) { this.referenced = referenced; }
    public void setXrefIndex(int xrefIndex) { this.xrefIndex = xrefIndex; }
    public void setXrefDependent(boolean xrefDependent) { this.xrefDependent = xrefDependent; }
    public void setXrefBlockHandle(DwgHandleRef handle) { this.xrefBlockHandle = handle; }
}
