package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgAttdef;
import io.dwg.sections.objects.ObjectWriter;

public class AttdefObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.ATTDEF.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgAttdef attdef = (DwgAttdef) source;
        Point3D pt = attdef.insertionPoint();
        w.write3RawDouble(new double[]{pt.x(), pt.y(), pt.z()});
        w.writeVariableText(attdef.tag());
        w.writeVariableText(attdef.prompt());
        w.writeVariableText(attdef.defaultValue());
        w.writeVariableText(attdef.textString());
        w.writeBitShort(attdef.flags());
        w.writeBitShort(attdef.justification());
        w.writeBitDouble(attdef.textHeight());
        w.writeBitDouble(attdef.rotation());
        w.writeBitDouble(attdef.widthFactor());
        w.writeBitDouble(attdef.obliquingAngle());
    }
}
