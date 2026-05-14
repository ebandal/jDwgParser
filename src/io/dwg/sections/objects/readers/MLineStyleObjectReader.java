package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgMLineStyle;
import io.dwg.sections.objects.ObjectReader;

/**
 * MLINESTYLE (복선 스타일) 테이블 엔트리 ObjectReader.
 * 타입 0x3D
 */
public class MLineStyleObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.MLINESTYLE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgMLineStyle mlStyle = (DwgMLineStyle) target;

        mlStyle.setName(r.readVariableText());
        mlStyle.setDescription(r.readVariableText());
        mlStyle.setFlags(r.readBitShort());
    }
}
