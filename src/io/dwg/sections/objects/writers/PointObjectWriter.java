package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgPoint;
import io.dwg.sections.objects.ObjectWriter;

public class PointObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.POINT.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgPoint point = (DwgPoint) source;
        Point3D pos = point.position();
        w.write3RawDouble(new double[]{pos.x(), pos.y(), pos.z()});
        w.writeBitThickness(point.thickness());
        w.writeBitExtrusion(point.extrusion());
        w.writeBitDouble(point.xAxisAngle());
    }
}
