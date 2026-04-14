package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgFace3D;
import io.dwg.sections.objects.ObjectReader;

/**
 * 3DFACE 엔티티 리더 (타입 0x1C)
 * 3개 또는 4개의 3D 점으로 정의된 면
 */
public class Face3DObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.FACE3D.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgFace3D face = (DwgFace3D) target;

        // 4개의 3D 점 읽기
        Point3D[] points = new Point3D[4];

        if (v.until(DwgVersion.R14)) {
            // R13/R14: 각 점을 개별적으로 읽음
            for (int i = 0; i < 4; i++) {
                double[] p = r.read3RawDouble();
                points[i] = new Point3D(p[0], p[1], p[2]);
            }
        } else {
            // R2000+: 압축된 형식
            double[] firstPt = r.read3RawDouble();
            double firstX = firstPt[0];
            double firstY = firstPt[1];
            double firstZ = firstPt[2];

            // 나머지 3개의 점을 상대 좌표로 읽음
            double[] p1 = r.read3RawDouble();
            double[] p2 = r.read3RawDouble();
            double[] p3 = r.read3RawDouble();

            points[0] = new Point3D(firstX, firstY, firstZ);
            points[1] = new Point3D(firstX + p1[0], firstY + p1[1], firstZ + p1[2]);
            points[2] = new Point3D(firstX + p2[0], firstY + p2[1], firstZ + p2[2]);
            points[3] = new Point3D(firstX + p3[0], firstY + p3[1], firstZ + p3[2]);
        }

        face.setPoints(points);
    }
}
