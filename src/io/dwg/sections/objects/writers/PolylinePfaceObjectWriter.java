package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgPolylinePface;
import io.dwg.sections.objects.ObjectWriter;

public class PolylinePfaceObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.POLYLINE_PFACE.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgPolylinePface polyline = (DwgPolylinePface) source;
        w.writeBitShort(polyline.numVertices());
        w.writeBitShort(polyline.numFaces());
        w.writeBitExtrusion(polyline.extrusion());
    }
}
