package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgOle2frame;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * OLE2FRAME (a class), spec §20.4.88: flags BS, R2000+ mode BS, data length BL, the OLE data,
 * R2000+ an unknown RC.
 */
public class Ole2frameObjectReader implements ObjectReader {
    @Override
    public int objectType() { return DwgObjectType.OLE2FRAME.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);
        DwgOle2frame ole = (DwgOle2frame) target;
        ole.setFlags(r.readBitShort());
        if (v.from(DwgVersion.R2000)) {
            ole.setMode(r.readBitShort());
        }
        int length = r.readBitLong();
        if (length < 0) {
            throw new IllegalStateException("negative OLE data length " + length);
        }
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = (byte) r.readRawChar();
        }
        ole.setOleData(data);
        if (v.from(DwgVersion.R2000)) {
            r.readRawChar();                       // unknown
        }
    }
}
