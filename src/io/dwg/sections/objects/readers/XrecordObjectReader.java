package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgXrecord;
import io.dwg.sections.objects.ObjectReader;

/**
 * XRECORD 엔티티 리더 (타입 0x4D)
 * 확장 데이터 레코드
 */
public class XrecordObjectReader implements ObjectReader {
    @Override
    public int objectType() { return DwgObjectType.XRECORD.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgXrecord xrecord = (DwgXrecord) target;

        // 1. recordType (BS)
        int recType = r.readBitShort();
        xrecord.setRecordType(recType);

        // 2. recordData (variable length binary)
        long dataLength = r.readBitLong();
        byte[] data = new byte[(int)dataLength];
        for (int i = 0; i < dataLength; i++) {
            data[i] = (byte) r.getInput().readBits(8);
        }
        xrecord.setRecordData(data);
    }
}
