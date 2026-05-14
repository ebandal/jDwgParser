package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.sections.objects.ObjectWriter;

public class BlockEndObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.BLOCK_END.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        // BLOCK_END에는 추가 데이터 없음
    }
}
