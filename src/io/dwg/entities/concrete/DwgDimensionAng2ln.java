package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgDimension;
import io.dwg.entities.DwgObjectType;

/** DIMENSION_ANG_2LN (type 24, spec §20.4.27): the common dimension data and points 16, 13, 14, 15, 10. */
public class DwgDimensionAng2ln extends AbstractDwgDimension {

    @Override
    public DwgObjectType objectType() { return DwgObjectType.DIMENSION_ANG_2LN; }
}
