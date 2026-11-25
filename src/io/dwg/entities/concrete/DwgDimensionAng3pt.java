package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgDimension;
import io.dwg.entities.DwgObjectType;

/** DIMENSION_ANG_3PT (type 23, spec §20.4.26): the common dimension data and points 10, 13, 14, 15. */
public class DwgDimensionAng3pt extends AbstractDwgDimension {

    @Override
    public DwgObjectType objectType() { return DwgObjectType.DIMENSION_ANG_3PT; }
}
