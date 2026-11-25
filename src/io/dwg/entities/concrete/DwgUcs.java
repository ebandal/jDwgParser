package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgTableRecord;
import io.dwg.entities.DwgObjectType;

/** UCS table record (type 63, spec §20.4.62): a named coordinate system. All fields are in {@link #values()}. */
public class DwgUcs extends AbstractDwgTableRecord {

    @Override
    public DwgObjectType objectType() { return DwgObjectType.UCS; }

    /** DXF 10 */
    public Point3D origin() { return pointValue("ORIGIN"); }
    /** DXF 11 */
    public Point3D xDirection() { return pointValue("XDIR"); }
    /** DXF 12 */
    public Point3D yDirection() { return pointValue("YDIR"); }
}
