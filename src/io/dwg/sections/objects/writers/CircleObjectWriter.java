package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgCircle;
import io.dwg.sections.objects.ObjectWriter;

public class CircleObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.CIRCLE.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgCircle circle = (DwgCircle) source;
        Point3D center = circle.center();
        w.write3RawDouble(new double[]{center.x(), center.y(), center.z()});
        w.writeBitDouble(circle.radius());
        w.writeBitThickness(circle.thickness());
        w.writeBitExtrusion(circle.extrusion());
    }
}
