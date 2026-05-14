package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgVertexPfaceFace;
import io.dwg.sections.objects.ObjectWriter;

public class VertexPfaceFaceObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.VERTEX_PFACE_FACE.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgVertexPfaceFace vertex = (DwgVertexPfaceFace) source;
        w.writeBitShort(vertex.vertex1Index());
        w.writeBitShort(vertex.vertex2Index());
        w.writeBitShort(vertex.vertex3Index());
        w.writeBitShort(vertex.vertex4Index());
    }
}
