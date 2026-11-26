package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgXLine;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * XLINE (type 41), spec §20.4.43: the common entity data, a point (3BD) and a
 * direction vector (3BD).
 */
public class XLineObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.XLINE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);

        DwgXLine xline = (DwgXLine) target;
        double[] point = r.read3BitDouble();
        xline.setStart(Point3D.of(point[0], point[1], point[2]));
        double[] vector = r.read3BitDouble();
        xline.setDirection(Point3D.of(vector[0], vector[1], vector[2]));
    }
}
