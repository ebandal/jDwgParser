package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgMText;
import io.dwg.sections.objects.ObjectWriter;

public class MTextObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.MTEXT.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgMText mtext = (DwgMText) source;
        Point3D pt = mtext.location();
        w.write3RawDouble(new double[]{pt.x(), pt.y(), pt.z()});
        w.writeBitDouble(mtext.width());
        w.writeBitDouble(mtext.height());
        w.writeVariableText(mtext.text());
        w.writeBitShort(mtext.attachmentPoint());
        w.writeBitDouble(mtext.angle());
        w.writeBitExtrusion(mtext.extrusion());
    }
}
