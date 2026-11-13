package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgSolid;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * SOLID 엔티티 리더 (타입 0x1F)
 * 3개 또는 4개의 점으로 정의된 채워진 다각형
 */
public class SolidObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.SOLID.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);

        DwgSolid solid = (DwgSolid) target;

        Point3D[] points = new Point3D[4];

        // thickness (BT), elevation (BD), 4 corners (2RD each, absolute, at the elevation)
        r.readBitThickness();
        double elevation = r.readBitDouble();
        for (int i = 0; i < 4; i++) {
            double[] p = r.read2RawDouble();
            points[i] = new Point3D(p[0], p[1], elevation);
        }

        solid.setPoints(points);
        solid.setExtrusion(r.readBitExtrusion());
    }
}
