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
    public static final class Wire {
        private final int type;
        private final int selectionMarker;
        private final int color;
        private final int acisIndex;
        private final List<Point3D> points;
        private final double[] transform;

        public Wire(int type, int selectionMarker, int color, int acisIndex, List<Point3D> points, double[] transform) {
            this.type = type;
            this.selectionMarker = selectionMarker;
            this.color = color;
            this.acisIndex = acisIndex;
            this.points = points;
            this.transform = transform;
        }

        public int type() {
            return type;
        }

        public int selectionMarker() {
            return selectionMarker;
        }

        public int color() {
            return color;
        }

        public int acisIndex() {
            return acisIndex;
        }

        public List<Point3D> points() {
            return points;
        }

        public double[] transform() {
            return transform;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Wire)) return false;
            Wire other = (Wire) o;
            return type == other.type
                    && selectionMarker == other.selectionMarker
                    && color == other.color
                    && acisIndex == other.acisIndex
                    && java.util.Objects.equals(points, other.points)
                    && java.util.Objects.equals(transform, other.transform);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(type, selectionMarker, color, acisIndex, points, transform);
        }

        @Override
        public String toString() {
            return "Wire[" + "type=" + type + ", " + "selectionMarker=" + selectionMarker + ", " + "color=" + color + ", " + "acisIndex=" + acisIndex + ", " + "points=" + points + ", " + "transform=" + transform + "]";
        }
    }

    /** The wires seen from one viewport. */
    public static final class Silhouette {
        private final int viewportId;
        private final Point3D target;
        private final Point3D direction;
        private final Point3D up;
        private final boolean perspective;
        private final List<Wire> wires;

        public Silhouette(int viewportId, Point3D target, Point3D direction, Point3D up, boolean perspective, List<Wire> wires) {
            this.viewportId = viewportId;
            this.target = target;
            this.direction = direction;
            this.up = up;
            this.perspective = perspective;
            this.wires = wires;
        }

        public int viewportId() {
            return viewportId;
        }

        public Point3D target() {
            return target;
        }

        public Point3D direction() {
            return direction;
        }

        public Point3D up() {
            return up;
        }

        public boolean perspective() {
            return perspective;
        }

        public List<Wire> wires() {
            return wires;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Silhouette)) return false;
            Silhouette other = (Silhouette) o;
            return viewportId == other.viewportId
                    && java.util.Objects.equals(target, other.target)
                    && java.util.Objects.equals(direction, other.direction)
                    && java.util.Objects.equals(up, other.up)
                    && perspective == other.perspective
                    && java.util.Objects.equals(wires, other.wires);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(viewportId, target, direction, up, perspective, wires);
        }

        @Override
        public String toString() {
            return "Silhouette[" + "viewportId=" + viewportId + ", " + "target=" + target + ", " + "direction=" + direction + ", " + "up=" + up + ", " + "perspective=" + perspective + ", " + "wires=" + wires + "]";
        }
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
    private java.util.UUID guid;

    /** The ACIS empty bit: no model in the object. R2013+ sets it and keeps the model in the data storage section; {@code DwgReader} then attaches it. */
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
    /** R2013+: the GUID stored after the wireframe (it can be the null GUID); {@code null} before R2013. */
    public java.util.UUID guid() { return guid; }

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
    public void setGuid(java.util.UUID guid) { this.guid = guid; }
}
