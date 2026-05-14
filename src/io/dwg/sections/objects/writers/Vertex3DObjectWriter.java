package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgVertex3D;
import io.dwg.sections.objects.ObjectWriter;

public class Vertex3DObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.VERTEX_3D.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgVertex3D vertex = (DwgVertex3D) source;
        Point3D loc = vertex.location();
        w.write3RawDouble(new double[]{loc.x(), loc.y(), loc.z()});
        w.writeBitShort(vertex.flags());
    }
}
