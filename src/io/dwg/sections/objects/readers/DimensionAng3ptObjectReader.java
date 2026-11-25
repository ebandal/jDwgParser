package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.AbstractDwgDimension;
import io.dwg.entities.DwgObjectType;

/** DIMENSION_ANG_3PT, spec §20.4.26: 10, 13, 14, 15 points (3BD). */
public class DimensionAng3ptObjectReader extends AbstractDimensionReader {

    @Override
    public int objectType() { return DwgObjectType.DIMENSION_ANG_3PT.typeCode(); }

    @Override
    protected void readTypeFields(AbstractDwgDimension dim, BitStreamReader r, DwgVersion v, double elevation) {
        dim.setPoint10(point(r));
        dim.setPoint13(point(r));
        dim.setPoint14(point(r));
        dim.setPoint15(point(r));
    }
}
