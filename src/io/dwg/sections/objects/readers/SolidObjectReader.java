package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgSolid;
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
        DwgSolid solid = (DwgSolid) target;

        // 4개의 점 읽기 (R2004 이상에서는 압축됨)
        Point3D[] points = new Point3D[4];

        if (v.until(DwgVersion.R14)) {
            // R13/R14: 각 점을 개별적으로 읽음
            for (int i = 0; i < 4; i++) {
                double[] p = r.read2RawDouble();
                points[i] = new Point3D(p[0], p[1], 0.0);
            }
        } else {
            // R2000+: 압축된 형식
            double[] firstX = r.read2RawDouble();
            double firstXVal = firstX[0];
            double firstYVal = firstX[1];

            // 나머지 3개의 점을 상대 좌표로 읽음
            double[] p1 = r.read2RawDouble();
            double[] p2 = r.read2RawDouble();
            double[] p3 = r.read2RawDouble();

            points[0] = new Point3D(firstXVal, firstYVal, 0.0);
            points[1] = new Point3D(firstXVal + p1[0], firstYVal + p1[1], 0.0);
            points[2] = new Point3D(firstXVal + p2[0], firstYVal + p2[1], 0.0);
            points[3] = new Point3D(firstXVal + p3[0], firstYVal + p3[1], 0.0);
        }

        solid.setPoints(points);
        solid.setExtrusion(r.readBitExtrusion());
    }
}
