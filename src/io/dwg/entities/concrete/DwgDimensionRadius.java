package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgDimension;
import io.dwg.entities.DwgObjectType;

/** DIMENSION_RADIUS (type 25, spec §20.4.28): the common dimension data and points 10, 15 and the leader length. */
public class DwgDimensionRadius extends AbstractDwgDimension {

    @Override
    public DwgObjectType objectType() { return DwgObjectType.DIMENSION_RADIUS; }
}
