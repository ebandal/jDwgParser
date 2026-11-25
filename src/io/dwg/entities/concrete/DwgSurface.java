package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * SURFACE (a class). The specification does not describe its data; the entity keeps its common
 * entity data (layer, color, ...) and its bytes ({@link #rawData()}).
 */
public class DwgSurface extends AbstractDwgEntity {

    @Override
    public DwgObjectType objectType() { return DwgObjectType.SURFACE; }
}
