package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.AbstractDwgDimension;
import io.dwg.entities.DwgObjectType;

/** DIMENSION_ORDINATE, spec §20.4.23: 10 point, 13 point, 14 point (3BD), flags 2 RC. */
public class DimensionOrdinateObjectReader extends AbstractDimensionReader {

    @Override
    public int objectType() { return DwgObjectType.DIMENSION_ORDINATE.typeCode(); }

    @Override
    protected void readTypeFields(AbstractDwgDimension dim, BitStreamReader r, DwgVersion v, double elevation) {
        dim.setPoint10(point(r));
        dim.setPoint13(point(r));
        dim.setPoint14(point(r));
        dim.setFlags2(r.readRawChar());
    }
}
