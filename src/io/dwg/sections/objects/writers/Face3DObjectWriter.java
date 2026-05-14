package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgFace3D;
import io.dwg.sections.objects.ObjectWriter;

public class Face3DObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.FACE3D.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgFace3D face = (DwgFace3D) source;
        Point3D[] pts = face.points();
        for (Point3D pt : pts) {
            w.write3RawDouble(new double[]{pt.x(), pt.y(), pt.z()});
        }
    }
}
