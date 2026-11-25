package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgObject;
import io.dwg.entities.DwgNonEntityObject;
import io.dwg.entities.DwgObjectType;

/**
 * TABLESTYLE (a class, spec §20.4.101). The fields are in {@link #values()} under the names of
 * {@code TableStyleObjectReader}: up to R2007 the description, margins and three row styles
 * (ROWSTYLES); from R2010 the cell styles (TABLECELLSTYLE and CELLSTYLES).
 */
public class DwgTableStyle extends AbstractDwgObject implements DwgNonEntityObject {

    @Override
    public DwgObjectType objectType() { return DwgObjectType.ACAD_TABLESTYLE; }

    @Override
    public boolean isEntity() { return false; }

    /** DXF 3 */
    public String description() { return value("DESCRIPTION") instanceof String ? (String) value("DESCRIPTION") : ""; }
}
