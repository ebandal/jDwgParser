package io.dwg.entities.concrete;

import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgTableRecord;
import io.dwg.entities.DwgObjectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * BLOCK_HEADER table record (type 49, spec §20.4.52): a block definition and the handles of
 * its parts.
 */
public class DwgBlockHeader extends AbstractDwgTableRecord {
    private int flags;
    private boolean loaded = true;
    private Point3D basePoint;
    private String xrefPath;
    private String description = "";
    private byte[] previewData;
    private int insertUnits;
    private boolean explodable = true;
    private int blockScaling;

    private DwgHandleRef blockEntityHandle;
    private DwgHandleRef firstEntityHandle;
    private DwgHandleRef lastEntityHandle;
    private final List<DwgHandleRef> ownedEntityHandles = new io.dwg.entities.CompactLists.Handles();
    private DwgHandleRef endBlockHandle;
    private final List<DwgHandleRef> insertHandles = new io.dwg.entities.CompactLists.Handles();
    private DwgHandleRef layoutHandle;

    // counts read from the data, for the handle stream
    private int ownedObjectCount;
    private int insertCount;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.BLOCK_HEADER; }

    /** Same as {@link #name()}. */
    public String blockName() { return name(); }
    /** 1 anonymous, 2 has attribute definitions, 4 xref, 8 overlaid xref (DXF 70). */
    public int flags() { return flags; }
    /** R2000+: false for an xref that is not loaded. */
    public boolean isLoaded() { return loaded; }
    public Point3D basePoint() { return basePoint; }
    public String xrefPath() { return xrefPath; }
    /** R2000+ */
    public String description() { return description; }
    /** R2000+: the preview image data as stored; {@code null} when there is none. */
    public byte[] previewData() { return previewData; }
    /** R2007+: the units of the block (DXF 70 of the BLOCK_RECORD). */
    public int insertUnits() { return insertUnits; }
    /** R2007+ */
    public boolean isExplodable() { return explodable; }
    /** R2007+: 1 when the block can only be scaled uniformly (DXF 281). */
    public int blockScaling() { return blockScaling; }

    /** The BLOCK entity that starts the definition. */
    public DwgHandleRef blockEntityHandle() { return blockEntityHandle; }
    /** R13–R2000, not for xrefs: the first entity of the definition. */
    public DwgHandleRef firstEntityHandle() { return firstEntityHandle; }
    /** R13–R2000, not for xrefs: the last entity of the definition. */
    public DwgHandleRef lastEntityHandle() { return lastEntityHandle; }
    /** R2004+: the entities of the definition. */
    public List<DwgHandleRef> ownedEntityHandles() { return Collections.unmodifiableList(ownedEntityHandles); }
    /** The ENDBLK entity that ends the definition. */
    public DwgHandleRef endBlockHandle() { return endBlockHandle; }
    /** R2000+: the INSERTs of this block. */
    public List<DwgHandleRef> insertHandles() { return Collections.unmodifiableList(insertHandles); }
    /** R2000+: the layout of a model or paper space block. */
    public DwgHandleRef layoutHandle() { return layoutHandle; }

    public int ownedObjectCount() { return ownedObjectCount; }
    public int insertCount() { return insertCount; }

    public void setBlockName(String blockName) { setName(blockName); }
    public void setFlags(int flags) { this.flags = flags; }
    public void setLoaded(boolean loaded) { this.loaded = loaded; }
    public void setBasePoint(Point3D basePoint) { this.basePoint = basePoint; }
    public void setXrefPath(String xrefPath) { this.xrefPath = xrefPath; }
    public void setDescription(String description) { this.description = description == null ? "" : description; }
    public void setPreviewData(byte[] data) { this.previewData = data; }
    public void setInsertUnits(int units) { this.insertUnits = units; }
    public void setExplodable(boolean explodable) { this.explodable = explodable; }
    public void setBlockScaling(int scaling) { this.blockScaling = scaling; }
    public void setBlockEntityHandle(DwgHandleRef h) { this.blockEntityHandle = h; }
    public void setFirstEntityHandle(DwgHandleRef h) { this.firstEntityHandle = h; }
    public void setLastEntityHandle(DwgHandleRef h) { this.lastEntityHandle = h; }
    public void addOwnedEntityHandle(DwgHandleRef h) { this.ownedEntityHandles.add(h); }
    public void setEndBlockHandle(DwgHandleRef h) { this.endBlockHandle = h; }
    public void addInsertHandle(DwgHandleRef h) { this.insertHandles.add(h); }
    public void setLayoutHandle(DwgHandleRef h) { this.layoutHandle = h; }
    public void setOwnedObjectCount(int count) { this.ownedObjectCount = count; }
    public void setInsertCount(int count) { this.insertCount = count; }

    public boolean isAnonymous() { return (flags & 0x01) != 0; }
    public boolean hasAttributes() { return (flags & 0x02) != 0; }
    public boolean isXref() { return (flags & 0x04) != 0; }
    public boolean isXrefOverlay() { return (flags & 0x08) != 0; }
}
