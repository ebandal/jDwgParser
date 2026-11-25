package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgAcisEntity;
import io.dwg.entities.DwgObjectType;

/** BODY (type 39, spec §20.4.41): an ACIS model; see {@link AbstractDwgAcisEntity}. */
public class DwgBody extends AbstractDwgAcisEntity {

    @Override
    public DwgObjectType objectType() { return DwgObjectType.BODY; }
}
