package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgVertex2D;
import io.dwg.sections.objects.ObjectWriter;

public class Vertex2DObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.VERTEX_2D.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgVertex2D vertex = (DwgVertex2D) source;
        Point3D loc = vertex.location();
        w.write3RawDouble(new double[]{loc.x(), loc.y(), loc.z()});
        w.writeBitDouble(vertex.startWidth());
        w.writeBitDouble(vertex.endWidth());
        w.writeBitDouble(vertex.bulge());
        w.writeBitShort(vertex.flags());
    }
}
