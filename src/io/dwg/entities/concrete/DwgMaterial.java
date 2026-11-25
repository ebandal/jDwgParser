package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgObject;
import io.dwg.entities.DwgNonEntityObject;
import io.dwg.entities.DwgObjectType;

/**
 * MATERIAL (a class). The specification does not describe its data; the object keeps its common
 * data (owner, reactors, extended data) and its bytes ({@link #rawData()}).
 */
public class DwgMaterial extends AbstractDwgObject implements DwgNonEntityObject {

    @Override
    public DwgObjectType objectType() { return DwgObjectType.ACAD_MATERIAL; }

    @Override
    public boolean isEntity() { return false; }
}
