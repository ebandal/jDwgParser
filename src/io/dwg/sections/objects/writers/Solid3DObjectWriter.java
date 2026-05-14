package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgSolid3d;
import io.dwg.sections.objects.ObjectWriter;

public class Solid3DObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.SOLID3D.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgSolid3d solid = (DwgSolid3d) source;
        w.writeBitLong(solid.numModelerFormatVersion());
        byte[] data = solid.modelerGeometryData();
        w.writeBitLong(data.length);
        for (byte b : data) {
            w.getOutput().writeBits(b & 0xFF, 8);
        }
    }
}
