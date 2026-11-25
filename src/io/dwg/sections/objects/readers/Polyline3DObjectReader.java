package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.AbstractDwgPolyline;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgPolyline3D;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * POLYLINE_3D 엔티티 리더 (타입 0x10)
 * 3D 폴리라인 (VERTEX_3D 객체들의 컨테이너)
 * 스펙 §20 POLYLINE_3D
 */
public class Polyline3DObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.POLYLINE_3D.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);

        DwgPolyline3D polyline = (DwgPolyline3D) target;
        polyline.setCurveType(r.getInput().readBits(8));    // spline flags (RC)
        polyline.setFlags(r.getInput().readBits(8));        // closed flags (RC) : 1 = closed
        PolylineHandles.readOwnedCount(polyline, r, v);
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        PolylineHandles.read((AbstractDwgPolyline) target, r, v);
    }
}
