package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgStyle;
import io.dwg.sections.objects.ObjectReader;

/**
 * STYLE (문자 스타일) 테이블 엔트리 ObjectReader.
 * 타입 0x34
 */
public class StyleObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.STYLE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgStyle style = (DwgStyle) target;

        style.setName(r.readVariableText());
        style.setWidth(r.readBitDouble());
        style.setOblique(r.readBitDouble());
        style.setFontFilename(r.readVariableText());
        style.setBigFontFilename(r.readVariableText());
        style.setFlags(r.readBitShort());
    }
}
