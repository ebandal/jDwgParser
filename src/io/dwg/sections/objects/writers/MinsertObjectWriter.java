package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgMinsert;
import io.dwg.sections.objects.ObjectWriter;

public class MinsertObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.MINSERT.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgMinsert minsert = (DwgMinsert) source;
        w.writeVariableText(minsert.blockName());
        Point3D pt = minsert.insertionPoint();
        w.write3RawDouble(new double[]{pt.x(), pt.y(), pt.z()});
        double[] scale = minsert.scale();
        w.write3BitDouble(scale);
        w.writeBitDouble(minsert.rotation());
        w.writeBitLong(minsert.rowCount());
        w.writeBitLong(minsert.columnCount());
        w.writeBitDouble(minsert.rowSpacing());
        w.writeBitDouble(minsert.columnSpacing());
        w.writeBitExtrusion(minsert.extrusion());
    }
}
