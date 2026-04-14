package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgBlockEnd;
import io.dwg.sections.objects.ObjectReader;

/**
 * BLOCK_END 엔티티 리더 (타입 0x31)
 * 블록 정의의 끝 마커 (데이터 없음)
 */
public class BlockEndObjectReader implements ObjectReader {
    @Override
    public int objectType() { return DwgObjectType.BLOCK_END.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        // BLOCK_END에는 추가 데이터가 없음
    }
}
