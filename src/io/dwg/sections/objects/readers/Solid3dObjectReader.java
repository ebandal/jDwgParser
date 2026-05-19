package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgSolid3d;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * SOLID3D 엔티티 리더 (타입 0x26)
 * 3D 솔리드 (ACIS 기하학 데이터)
 */
public class Solid3dObjectReader implements ObjectReader {
    @Override
    public int objectType() { return DwgObjectType.SOLID3D.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);

        DwgSolid3d solid = (DwgSolid3d) target;

        long version = r.readBitLong();
        solid.setNumModelerFormatVersion((int)version);

        long dataLength = r.readBitLong();
        if (dataLength > 0 && dataLength <= 0x100000L) {
            r.seek(r.position() + dataLength * 8L);
        }
    }
}
