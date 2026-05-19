package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgBody;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * BODY 엔티티 리더 (타입 0x27)
 * 3D 솔리드 바디 (모델러 기하학 데이터)
 */
public class BodyObjectReader implements ObjectReader {
    @Override
    public int objectType() { return DwgObjectType.BODY.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);

        DwgBody body = (DwgBody) target;

        long version = r.readBitLong();
        body.setNumModelerFormatVersion((int)version);

        long dataLength = r.readBitLong();
        if (dataLength > 0 && dataLength <= 0x100000L) {
            r.seek(r.position() + dataLength * 8L);
        }
    }
}
