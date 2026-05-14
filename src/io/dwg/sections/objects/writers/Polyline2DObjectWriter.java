package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgPolyline2D;
import io.dwg.sections.objects.ObjectWriter;

public class Polyline2DObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.POLYLINE_2D.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgPolyline2D polyline = (DwgPolyline2D) source;
        w.writeBitShort(polyline.flags());
        w.writeBitDouble(polyline.defaultStartWidth());
        w.writeBitDouble(polyline.defaultEndWidth());
        w.writeBitDouble(polyline.elevation());
        w.writeBitExtrusion(polyline.extrusion());
    }
}
