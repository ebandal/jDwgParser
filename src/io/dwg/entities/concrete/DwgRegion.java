package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgAcisEntity;
import io.dwg.entities.DwgObjectType;

/** REGION (type 37, spec §20.4.41): an ACIS model; see {@link AbstractDwgAcisEntity}. */
public class DwgRegion extends AbstractDwgAcisEntity {

    @Override
    public DwgObjectType objectType() { return DwgObjectType.REGION; }
}
