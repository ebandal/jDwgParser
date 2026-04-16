package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point2D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgLayout;
import io.dwg.sections.objects.ObjectReader;

/**
 * LAYOUT (페이지 레이아웃) 객체 ObjectReader.
 * 타입 0x50
 */
public class LayoutObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.LAYOUT.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgLayout layout = (DwgLayout) target;

        layout.setName(r.readVariableText());
        layout.setTabOrder(r.readBitShort());

        double[] paperSize2d = r.read2BitDouble();
        layout.setPaperSize(new Point2D(paperSize2d[0], paperSize2d[1]));

        layout.setMarginLeft(r.readBitDouble());
        layout.setMarginRight(r.readBitDouble());
        layout.setMarginTop(r.readBitDouble());
        layout.setMarginBottom(r.readBitDouble());
    }
}
