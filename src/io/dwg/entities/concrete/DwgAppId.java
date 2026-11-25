package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgTableRecord;
import io.dwg.entities.DwgObjectType;

/**
 * APPID table record (type 67, spec §20.4.66): the name of an application that attaches
 * extended data to objects.
 */
public class DwgAppId extends AbstractDwgTableRecord {
    private int unknown;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.APPID; }

    /** The undocumented RC after the flags (DXF group 71, written only when it is not 0). */
    public int flags() { return unknown; }

    public void setFlags(int value) { this.unknown = value; }
}
