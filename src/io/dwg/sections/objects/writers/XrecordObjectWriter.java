package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgXrecord;
import io.dwg.sections.objects.ObjectWriter;

public class XrecordObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.XRECORD.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgXrecord xrecord = (DwgXrecord) source;
        w.writeBitShort(xrecord.recordType());
        byte[] data = xrecord.recordData();
        w.writeBitLong(data.length);
        for (byte b : data) {
            w.getOutput().writeBits(b & 0xFF, 8);
        }
    }
}
