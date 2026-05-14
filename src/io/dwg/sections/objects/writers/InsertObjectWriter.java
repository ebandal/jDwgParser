package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgInsert;
import io.dwg.sections.objects.ObjectWriter;

public class InsertObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.INSERT.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgInsert insert = (DwgInsert) source;
        Point3D pt = insert.insertionPoint();
        w.write3RawDouble(new double[]{pt.x(), pt.y(), pt.z()});
        w.write3BitDouble(new double[]{insert.xScale(), insert.yScale(), insert.zScale()});
        w.writeBitDouble(insert.rotation());
        w.writeBitExtrusion(insert.extrusion());
    }
}
