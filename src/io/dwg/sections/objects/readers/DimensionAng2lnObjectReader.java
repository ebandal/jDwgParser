package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.AbstractDwgDimension;
import io.dwg.entities.DwgObjectType;

/** DIMENSION_ANG_2LN, spec §20.4.27: 16 point (2RD), 13, 14, 15, 10 points (3BD). */
public class DimensionAng2lnObjectReader extends AbstractDimensionReader {

    @Override
    public int objectType() { return DwgObjectType.DIMENSION_ANG_2LN.typeCode(); }

    @Override
    protected void readTypeFields(AbstractDwgDimension dim, BitStreamReader r, DwgVersion v, double elevation) {
        double[] p16 = r.read2RawDouble();
        dim.setPoint16(Point3D.of(p16[0], p16[1], elevation));
        dim.setPoint13(point(r));
        dim.setPoint14(point(r));
        dim.setPoint15(point(r));
        dim.setPoint10(point(r));
    }
}
