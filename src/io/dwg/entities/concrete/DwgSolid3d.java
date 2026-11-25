package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgAcisEntity;
import io.dwg.entities.DwgObjectType;

/** SOLID3D (type 38, spec §20.4.41): an ACIS model; see {@link AbstractDwgAcisEntity}. */
public class DwgSolid3d extends AbstractDwgAcisEntity {

    @Override
    public DwgObjectType objectType() { return DwgObjectType.SOLID3D; }
}
