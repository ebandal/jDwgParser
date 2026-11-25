package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.AbstractDwgDimension;
import io.dwg.entities.DwgObjectType;

/** DIMENSION_ALIGNED, spec §20.4.25: 13, 14, 10 points (3BD), extension line rotation BD. */
public class DimensionAlignedObjectReader extends AbstractDimensionReader {

    @Override
    public int objectType() { return DwgObjectType.DIMENSION_ALIGNED.typeCode(); }

    @Override
    protected void readTypeFields(AbstractDwgDimension dim, BitStreamReader r, DwgVersion v, double elevation) {
        dim.setPoint13(point(r));
        dim.setPoint14(point(r));
        dim.setPoint10(point(r));
        dim.setExtensionLineRotation(r.readBitDouble());
    }
}
