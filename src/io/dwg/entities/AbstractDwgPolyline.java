package io.dwg.entities;

import io.dwg.core.type.DwgHandleRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The handles every old-style POLYLINE (2D, 3D, PFACE, MESH) has to its vertices (spec §20.4.16,
 * §20.4.17, §20.4.33, §20.4.34): R13–R2000 the first and last VERTEX, R2004+ every VERTEX, and
 * the SEQEND that ends them.
 */
public abstract class AbstractDwgPolyline extends AbstractDwgEntity {
    private int ownedObjectCount;
    private DwgHandleRef firstVertexHandle;
    private DwgHandleRef lastVertexHandle;
    private final List<DwgHandleRef> vertexHandles = new ArrayList<>();
    private DwgHandleRef seqendHandle;

    /** R2004+: the number of vertices the polyline owns. */
    public int ownedObjectCount() { return ownedObjectCount; }
    /** R13–R2000 */
    public DwgHandleRef firstVertexHandle() { return firstVertexHandle; }
    /** R13–R2000 */
    public DwgHandleRef lastVertexHandle() { return lastVertexHandle; }
    /** R2004+: the vertices in order. */
    public List<DwgHandleRef> vertexHandles() { return Collections.unmodifiableList(vertexHandles); }
    public DwgHandleRef seqendHandle() { return seqendHandle; }

    public void setOwnedObjectCount(int n) { this.ownedObjectCount = n; }
    public void setFirstVertexHandle(DwgHandleRef h) { this.firstVertexHandle = h; }
    public void setLastVertexHandle(DwgHandleRef h) { this.lastVertexHandle = h; }
    public void addVertexHandle(DwgHandleRef h) { this.vertexHandles.add(h); }
    public void setSeqendHandle(DwgHandleRef h) { this.seqendHandle = h; }
}
