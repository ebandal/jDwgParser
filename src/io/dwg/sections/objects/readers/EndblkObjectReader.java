package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.sections.objects.ObjectReader;

/**
 * ENDBLK 오브젝트 리더 (타입 0x05)
 * 블록 정의의 끝을 표시하는 마커 (데이터 없음)
 */
public class EndblkObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.ENDBLK.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        // ENDBLK에는 추가 데이터가 없음
    }
}
