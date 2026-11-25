package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgDimension;
import io.dwg.entities.DwgObjectType;

/** DIMENSION_ALIGNED (type 22, spec §20.4.25): the common dimension data and points 13, 14, 10 and the extension line rotation. */
public class DwgDimensionAligned extends AbstractDwgDimension {

    @Override
    public DwgObjectType objectType() { return DwgObjectType.DIMENSION_ALIGNED; }
}
