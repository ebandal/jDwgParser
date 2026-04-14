package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgArc;
import io.dwg.sections.objects.ObjectWriter;

public class ArcObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.ARC.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgArc arc = (DwgArc) source;
        Point3D center = arc.center();
        w.write3RawDouble(new double[]{center.x(), center.y(), center.z()});
        w.writeBitDouble(arc.radius());
        w.writeBitThickness(arc.thickness());
        w.writeBitExtrusion(arc.extrusion());
        w.writeBitDouble(arc.startAngle());
        w.writeBitDouble(arc.endAngle());
    }
}
