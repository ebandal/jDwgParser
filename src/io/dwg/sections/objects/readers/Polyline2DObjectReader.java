package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgPolyline2D;
import io.dwg.sections.objects.ObjectReader;

/**
 * POLYLINE_2D 엔티티 리더 (타입 0x0F)
 * 2D 폴리라인 (VERTEX_2D 객체들의 컨테이너)
 * 스펙 §20 POLYLINE_2D
 */
public class Polyline2DObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.POLYLINE_2D.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgPolyline2D polyline = (DwgPolyline2D) target;

        // 1. flags (BS - bit short)
        int flags = r.readBitShort();
        polyline.setFlags(flags);

        // 2. defaultStartWidth (BD - bit double)
        double defaultStartWidth = r.readBitDouble();
        polyline.setDefaultStartWidth(defaultStartWidth);

        // 3. defaultEndWidth (BD)
        double defaultEndWidth = r.readBitDouble();
        polyline.setDefaultEndWidth(defaultEndWidth);

        // 4. elevation (BD)
        double elevation = r.readBitDouble();
        polyline.setElevation(elevation);

        // 5. extrusion (BE - bit extrusion)
        double[] extrusion = r.readBitExtrusion();
        polyline.setExtrusion(extrusion);
    }
}
