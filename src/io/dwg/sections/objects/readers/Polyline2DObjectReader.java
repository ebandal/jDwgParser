package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.AbstractDwgPolyline;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgPolyline2D;
import io.dwg.sections.objects.EntityHeaderReader;
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
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);

        DwgPolyline2D polyline = (DwgPolyline2D) target;
        polyline.setFlags(r.readBitShort());
        polyline.setCurveType(r.readBitShort());
        polyline.setDefaultStartWidth(r.readBitDouble());
        polyline.setDefaultEndWidth(r.readBitDouble());
        polyline.setThickness(r.readBitThickness());
        polyline.setElevation(r.readBitDouble());
        polyline.setExtrusion(r.readBitExtrusion());
        PolylineHandles.readOwnedCount(polyline, r, v);
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        PolylineHandles.read((AbstractDwgPolyline) target, r, v);
    }
}
