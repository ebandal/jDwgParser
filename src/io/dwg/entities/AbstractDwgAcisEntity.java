package io.dwg.entities;

import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point3D;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * REGION, 3DSOLID and BODY (spec §20.4.41): an ACIS model and the wireframe AutoCAD keeps to
 * draw it. The model is not interpreted here: it is kept as SAT text ({@link #satText()}) or SAB
 * bytes ({@link #sabData()}), ready to be handed to an ACIS-capable library.
 */
public abstract class AbstractDwgAcisEntity extends AbstractDwgEntity {

    /**
     * One wire of the wireframe. {@code transform} is {@code null} or 13 values: X axis, Y axis,
     * Z axis, translation (3 each) and the scale.
     */
    public record Wire(int type, int selectionMarker, int color, int acisIndex, List<Point3D> points,
                       double[] transform) {
    }

    /** The wires seen from one viewport. */
    public record Silhouette(int viewportId, Point3D target, Point3D direction, Point3D up,
                             boolean perspective, List<Wire> wires) {
    }

    private boolean empty = true;
    private int acisVersion;
    private String satText;
    private byte[] sabData;
    private Point3D wireframePoint;
    private int isolineCount;
    private final List<Wire> wires = new ArrayList<>();
    private final List<Silhouette> silhouettes = new ArrayList<>();
    private DwgHandleRef historyHandle;

    /** No ACIS data is in the object (R2013+ files may keep it in the data storage section). */
    public boolean isEmpty() { return empty; }
    /** 1: SAT text stored in blocks with its characters encoded; 2: a SAT or SAB file. */
    public int acisVersion() { return acisVersion; }
    /** The model as SAT text, or {@code null} when it is binary (SAB) or absent. */
    public String satText() { return satText; }
    /** The model as SAB bytes, or {@code null} when it is text or absent. */
    public byte[] sabData() { return sabData; }
    public Point3D wireframePoint() { return wireframePoint; }
    public int isolineCount() { return isolineCount; }
    public List<Wire> wires() { return Collections.unmodifiableList(wires); }
    public List<Silhouette> silhouettes() { return Collections.unmodifiableList(silhouettes); }
    /** R2007+: the history object (DXF 350). */
    public DwgHandleRef historyHandle() { return historyHandle; }

    /** Same as {@link #acisVersion()}. */
    public int numModelerFormatVersion() { return acisVersion; }

    /** The model as stored bytes: the SAB data, or the SAT text in ISO-8859-1; {@code null} if absent. */
    public byte[] modelerGeometryData() {
        if (sabData != null) return sabData;
        return satText == null ? null : satText.getBytes(StandardCharsets.ISO_8859_1);
    }

    public void setEmpty(boolean empty) { this.empty = empty; }
    public void setAcisVersion(int version) { this.acisVersion = version; }
    public void setSatText(String text) { this.satText = text; }
    public void setSabData(byte[] data) { this.sabData = data; }
    public void setWireframePoint(Point3D p) { this.wireframePoint = p; }
    public void setIsolineCount(int n) { this.isolineCount = n; }
    public void addWire(Wire w) { wires.add(w); }
    public void addSilhouette(Silhouette s) { silhouettes.add(s); }
    public void setHistoryHandle(DwgHandleRef h) { this.historyHandle = h; }
}
