package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgCircle;
import io.dwg.sections.objects.ObjectReader;

public class CircleObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.CIRCLE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgCircle circle = (DwgCircle) target;
        double[] center = r.read3BitDouble();
        circle.setCenter(new Point3D(center[0], center[1], center[2]));
        circle.setRadius(r.readBitDouble());
        circle.setThickness(r.readBitThickness());
        circle.setExtrusion(r.readBitExtrusion());
    }
}
