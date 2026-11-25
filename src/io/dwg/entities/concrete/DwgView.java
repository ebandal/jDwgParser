package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgTableRecord;
import io.dwg.entities.DwgObjectType;

/** VIEW table record (type 61, spec §20.4.60): a named view. All fields are in {@link #values()}. */
public class DwgView extends AbstractDwgTableRecord {

    @Override
    public DwgObjectType objectType() { return DwgObjectType.VIEW; }

    /** The view target (DXF 12). */
    public Point3D viewTarget() { return pointValue("TARGET"); }
    /** The direction from the target toward the camera (DXF 11). */
    public Point3D viewDirection() { return pointValue("VIEWDIR"); }
    /** DXF 40 */
    public double viewHeight() { return doubleValue("HEIGHT", 1.0); }
    /** DXF 41 */
    public double viewWidth() { return doubleValue("WIDTH", 1.0); }
    /** The view center in DCS (DXF 10). */
    public Point3D viewCenter() { return pointValue("CENTER"); }
}
