package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgWipeout;
import io.dwg.sections.objects.ObjectReader;

public class WipeoutObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.WIPEOUT.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgWipeout wipeout = (DwgWipeout) target;

        // Insertion point (3D vector)
        double[] point = r.read3BitDouble();
        wipeout.setInsertionPoint(new Point3D(point[0], point[1], point[2]));

        // U vector (direction along width)
        double[] uVec = r.read3BitDouble();
        wipeout.setUVector(uVec);

        // V vector (direction along height)
        double[] vVec = r.read3BitDouble();
        wipeout.setVVector(vVec);

        // Width and height
        wipeout.setWidth(r.readBitDouble());
        wipeout.setHeight(r.readBitDouble());

        // Clipping state
        wipeout.setClippingState(r.readBitShort());

        // Wipeout image type
        wipeout.setWipeoutImageType(r.readBitShort());
    }
}
