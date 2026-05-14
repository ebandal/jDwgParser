package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgPolylineMesh;
import io.dwg.sections.objects.ObjectWriter;

public class PolylineMeshObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.POLYLINE_MESH.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgPolylineMesh polyline = (DwgPolylineMesh) source;
        w.writeBitShort(polyline.flags());
        w.writeBitShort(polyline.mVertexCount());
        w.writeBitShort(polyline.nVertexCount());
        w.writeBitShort(polyline.mDensity());
        w.writeBitShort(polyline.nDensity());
        w.writeBitExtrusion(polyline.extrusion());
    }
}
