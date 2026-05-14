package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgPolyline3D;
import io.dwg.sections.objects.ObjectWriter;

public class Polyline3DObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.POLYLINE_3D.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgPolyline3D polyline = (DwgPolyline3D) source;
        w.writeBitShort(polyline.flags());
    }
}
