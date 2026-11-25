package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgObject;
import io.dwg.entities.DwgNonEntityObject;
import io.dwg.entities.DwgObjectType;

/**
 * ACDBPERSSUBENTMANAGER (a class). The specification does not describe its data; the object keeps its common
 * data (owner, reactors, extended data) and its bytes ({@link #rawData()}).
 */
public class DwgPersSubentManager extends AbstractDwgObject implements DwgNonEntityObject {

    @Override
    public DwgObjectType objectType() { return DwgObjectType.ACAD_PERSSUBENTMANAGER; }

    @Override
    public boolean isEntity() { return false; }
}
