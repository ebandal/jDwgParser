package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgLtype;
import io.dwg.sections.objects.ObjectReader;

/**
 * LTYPE (선 유형) 테이블 엔트리 ObjectReader.
 * 타입 0x32
 */
public class LtypeObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.LTYPE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgLtype ltype = (DwgLtype) target;

        ltype.setName(r.readVariableText());
        ltype.setDescription(r.readVariableText());
        ltype.setTotalLength(r.readBitDouble());
        ltype.setNumDashes(r.readBitShort());
    }
}
