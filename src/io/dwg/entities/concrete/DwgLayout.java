package io.dwg.entities.concrete;

import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point2D;
import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgObject;
import io.dwg.entities.DwgNonEntityObject;
import io.dwg.entities.DwgObjectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * LAYOUT (a class, spec §20.4.84): a layout tab and its plot settings. All fields are in
 * {@link #values()} under the names of {@code LayoutObjectReader}; the main ones have getters.
 */
public class DwgLayout extends AbstractDwgObject implements DwgNonEntityObject {
    private final List<DwgHandleRef> viewportHandles = new ArrayList<>();

    @Override
    public DwgObjectType objectType() { return DwgObjectType.LAYOUT; }

    @Override
    public boolean isEntity() { return false; }

    /** The layout name (DXF 1). */
    public String name() { return value("LAYOUTNAME") instanceof String ? (String) value("LAYOUTNAME") : ""; }
    /** The tab order (DXF 71); 0 is model space. */
    public int tabOrder() { return value("TABORDER") instanceof Integer ? (Integer) value("TABORDER") : 0; }
    /** The paper size in millimeters. */
    public Point2D paperSize() { return new Point2D(number("PAPERWIDTH"), number("PAPERHEIGHT")); }
    public double marginLeft() { return number("LEFTMARGIN"); }
    public double marginRight() { return number("RIGHTMARGIN"); }
    public double marginTop() { return number("TOPMARGIN"); }
    public double marginBottom() { return number("BOTTOMMARGIN"); }
    public Point3D limitsMin() { return point("LIMMIN"); }
    public Point3D limitsMax() { return point("LIMMAX"); }
    public Point3D extentsMin() { return point("EXTMIN"); }
    public Point3D extentsMax() { return point("EXTMAX"); }
    /** The paper space BLOCK_HEADER of this layout (DXF 330). */
    public DwgHandleRef blockHandle() { return handle("BLOCK"); }
    /** The last active viewport (DXF 331). */
    public DwgHandleRef lastActiveViewportHandle() { return handle("LASTVIEWPORT"); }
    /** R2004+: the viewports of the layout. */
    public List<DwgHandleRef> viewportHandles() { return Collections.unmodifiableList(viewportHandles); }

    public void addViewportHandle(DwgHandleRef h) { viewportHandles.add(h); }

    private double number(String name) {
        Object v = value(name);
        return v instanceof Number ? ((Number) v).doubleValue() : 0;
    }

    private Point3D point(String name) {
        Object v = value(name);
        return v instanceof Point3D ? (Point3D) v : null;
    }

    private DwgHandleRef handle(String name) {
        Object v = value(name);
        return v instanceof DwgHandleRef ? (DwgHandleRef) v : null;
    }
}
