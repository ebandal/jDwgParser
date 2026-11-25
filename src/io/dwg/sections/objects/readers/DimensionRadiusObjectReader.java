package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.AbstractDwgDimension;
import io.dwg.entities.DwgObjectType;

/** DIMENSION_RADIUS, spec §20.4.28: 10, 15 points (3BD), leader length BD. */
public class DimensionRadiusObjectReader extends AbstractDimensionReader {

    @Override
    public int objectType() { return DwgObjectType.DIMENSION_RADIUS.typeCode(); }

    @Override
    protected void readTypeFields(AbstractDwgDimension dim, BitStreamReader r, DwgVersion v, double elevation) {
        dim.setPoint10(point(r));
        dim.setPoint15(point(r));
        dim.setLeaderLength(r.readBitDouble());
    }
}
