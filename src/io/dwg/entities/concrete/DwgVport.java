package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgTableRecord;
import io.dwg.entities.DwgObjectType;

/** VPORT table record (type 65, spec §20.4.64): a model space viewport configuration. All fields are in {@link #values()}. */
public class DwgVport extends AbstractDwgTableRecord {

    @Override
    public DwgObjectType objectType() { return DwgObjectType.VPORT; }

    /** The view center in DCS (DXF 12). */
    public Point3D viewCenter() { return pointValue("CENTER"); }
    /** DXF 40 */
    public double viewHeight() { return doubleValue("HEIGHT", 1.0); }
    /** DXF 13 */
    public Point3D snapBase() { return pointValue("SNAPBASE"); }
    /** DXF 15 */
    public double gridSpacingX() { return pointValue("GRIDSPACING") == null ? 1.0 : pointValue("GRIDSPACING").x(); }
    /** DXF 25 */
    public double gridSpacingY() { return pointValue("GRIDSPACING") == null ? 1.0 : pointValue("GRIDSPACING").y(); }
}
