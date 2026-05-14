package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgOle2frame;
import io.dwg.sections.objects.ObjectWriter;

public class Ole2frameObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.OLE2FRAME.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgOle2frame ole = (DwgOle2frame) source;
        Point3D pt = ole.insertionPoint();
        w.write3RawDouble(new double[]{pt.x(), pt.y(), pt.z()});
        double[] scale = ole.scale();
        w.write3BitDouble(scale);
        w.writeBitDouble(ole.rotation());
        w.writeVariableText(ole.oleName());
        byte[] oleData = ole.oleData();
        w.writeBitLong(oleData.length);
        for (byte b : oleData) {
            w.getOutput().writeBits(b & 0xFF, 8);
        }
    }
}
