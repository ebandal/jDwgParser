package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgAttrib;
import io.dwg.sections.objects.ObjectWriter;

public class AttribObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.ATTRIB.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgAttrib attrib = (DwgAttrib) source;
        Point3D pt = attrib.location();
        w.write3RawDouble(new double[]{pt.x(), pt.y(), pt.z()});
        w.writeBitDouble(attrib.height());
        w.writeVariableText(attrib.text());
        w.writeBitDouble(attrib.angle());
    }
}
