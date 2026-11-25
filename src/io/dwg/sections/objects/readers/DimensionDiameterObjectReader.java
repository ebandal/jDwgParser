package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.AbstractDwgDimension;
import io.dwg.entities.DwgObjectType;

/** DIMENSION_DIAMETER, spec §20.4.29: 15, 10 points (3BD), leader length BD. */
public class DimensionDiameterObjectReader extends AbstractDimensionReader {

    @Override
    public int objectType() { return DwgObjectType.DIMENSION_DIAMETER.typeCode(); }

    @Override
    protected void readTypeFields(AbstractDwgDimension dim, BitStreamReader r, DwgVersion v, double elevation) {
        dim.setPoint15(point(r));
        dim.setPoint10(point(r));
        dim.setLeaderLength(r.readBitDouble());
    }
}
