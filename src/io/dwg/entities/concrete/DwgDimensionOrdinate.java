package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgDimension;
import io.dwg.entities.DwgObjectType;

/** DIMENSION_ORDINATE (type 20, spec §20.4.23): the common dimension data and points 10, 13, 14 and flags 2. */
public class DwgDimensionOrdinate extends AbstractDwgDimension {

    @Override
    public DwgObjectType objectType() { return DwgObjectType.DIMENSION_ORDINATE; }
}
