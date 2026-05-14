package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgRay;
import io.dwg.sections.objects.ObjectWriter;

public class RayObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.RAY.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgRay ray = (DwgRay) source;
        Point3D pt = ray.start();
        w.write3RawDouble(new double[]{pt.x(), pt.y(), pt.z()});
        Point3D dir = ray.direction();
        w.write3RawDouble(new double[]{dir.x(), dir.y(), dir.z()});
    }
}
