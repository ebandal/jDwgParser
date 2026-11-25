package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgDimension;
import io.dwg.entities.DwgObjectType;

/** DIMENSION_LINEAR (type 21, spec §20.4.24): the common dimension data and points 13, 14, 10, the extension line and dimension rotations. */
public class DwgDimensionLinear extends AbstractDwgDimension {

    @Override
    public DwgObjectType objectType() { return DwgObjectType.DIMENSION_LINEAR; }
}
