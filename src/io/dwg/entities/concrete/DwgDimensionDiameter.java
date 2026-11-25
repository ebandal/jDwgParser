package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgDimension;
import io.dwg.entities.DwgObjectType;

/** DIMENSION_DIAMETER (type 26, spec §20.4.29): the common dimension data and points 15, 10 and the leader length. */
public class DwgDimensionDiameter extends AbstractDwgDimension {

    @Override
    public DwgObjectType objectType() { return DwgObjectType.DIMENSION_DIAMETER; }
}
