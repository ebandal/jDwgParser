package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgSeqEnd;
import io.dwg.sections.objects.ObjectReader;

/**
 * SEQEND 엔티티 리더 (타입 0x04)
 * POLYLINE/SHAPE의 끝을 표시하는 마커 (데이터 없음)
 */
public class SeqEndObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.SEQEND.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        // SEQEND에는 추가 데이터가 없음
    }
}
