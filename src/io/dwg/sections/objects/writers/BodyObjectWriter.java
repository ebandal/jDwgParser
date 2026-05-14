package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgBody;
import io.dwg.sections.objects.ObjectWriter;

public class BodyObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.BODY.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgBody body = (DwgBody) source;
        w.writeBitLong(body.numModelerFormatVersion());
        byte[] data = body.modelerGeometryData();
        w.writeBitLong(data.length);
        for (byte b : data) {
            w.getOutput().writeBits(b & 0xFF, 8);
        }
    }
}
