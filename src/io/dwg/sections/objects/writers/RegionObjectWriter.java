package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgRegion;
import io.dwg.sections.objects.ObjectWriter;

public class RegionObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.REGION.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgRegion region = (DwgRegion) source;
        w.writeBitLong(region.numModelerFormatVersion());
        byte[] data = region.modelerGeometryData();
        w.writeBitLong(data.length);
        for (byte b : data) {
            w.getOutput().writeBits(b & 0xFF, 8);
        }
    }
}
