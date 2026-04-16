package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgDimStyle;
import io.dwg.sections.objects.ObjectReader;

/**
 * DIMSTYLE (치수 스타일) 테이블 엔트리 ObjectReader.
 * 타입 0x3A
 */
public class DimStyleObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.DIMSTYLE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgDimStyle dimStyle = (DwgDimStyle) target;

        dimStyle.setName(r.readVariableText());
        dimStyle.setTextHeight(r.readBitDouble());
        dimStyle.setTextGap(r.readBitDouble());
        dimStyle.setArrowSize(r.readBitDouble());
        dimStyle.setLineExtension(r.readBitDouble());
        dimStyle.setLineOffset(r.readBitDouble());
    }
}
