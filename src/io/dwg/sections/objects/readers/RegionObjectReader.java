package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgRegion;
import io.dwg.sections.objects.ObjectReader;

/**
 * REGION 엔티티 리더 (타입 0x25)
 * 영역 (모델러 기하학 데이터)
 */
public class RegionObjectReader implements ObjectReader {
    @Override
    public int objectType() { return DwgObjectType.REGION.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgRegion region = (DwgRegion) target;

        // 1. numModelerFormatVersion (BL)
        long version = r.readBitLong();
        region.setNumModelerFormatVersion((int)version);

        // 2. modelerGeometryData (SAT data - variable length)
        long dataLength = r.readBitLong();
        byte[] data = new byte[(int)dataLength];
        for (int i = 0; i < dataLength; i++) {
            data[i] = (byte) r.getInput().readBits(8);
        }
        region.setModelerGeometryData(data);
    }
}
