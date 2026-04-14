package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.sections.objects.ObjectWriter;

public class SeqEndObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.SEQEND.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        // SEQEND에는 추가 데이터가 없음
    }
}
