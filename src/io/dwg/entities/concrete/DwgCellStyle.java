package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgObject;
import io.dwg.entities.DwgNonEntityObject;
import io.dwg.entities.DwgObjectType;

/**
 * CELLSTYLEMAP (a class, spec §20.4.102): the cell styles of a table style, in {@link #values()}
 * as CELLSTYLES, a list of maps (see {@code CellStyles}).
 */
public class DwgCellStyle extends AbstractDwgObject implements DwgNonEntityObject {

    @Override
    public DwgObjectType objectType() { return DwgObjectType.ACAD_CELLSTYLE; }

    @Override
    public boolean isEntity() { return false; }
}
