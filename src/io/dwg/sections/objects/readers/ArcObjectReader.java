package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgArc;
import io.dwg.sections.objects.ObjectReader;

public class ArcObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.ARC.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgArc arc = (DwgArc) target;
        double[] center = r.read3BitDouble();
        arc.setCenter(new Point3D(center[0], center[1], center[2]));
        arc.setRadius(r.readBitDouble());
        arc.setThickness(r.readBitThickness());
        arc.setExtrusion(r.readBitExtrusion());
        arc.setStartAngle(r.readBitDouble());
        arc.setEndAngle(r.readBitDouble());
    }
}
