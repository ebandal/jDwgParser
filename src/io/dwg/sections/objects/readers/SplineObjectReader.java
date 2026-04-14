package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgSpline;
import io.dwg.sections.objects.ObjectReader;

/**
 * SPLINE 엔티티 리더 (타입 0x24)
 * 제어점을 기반으로 한 B-spline 곡선
 */
public class SplineObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.SPLINE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgSpline spline = (DwgSpline) target;

        // 노멀 벡터
        double[] extrusion = r.readBitExtrusion();
        spline.setExtrusion(extrusion);

        // B-spline 차수
        int degree = r.getInput().readRawShort();
        spline.setDegree(degree);

        // 플래그
        int flags = r.getInput().readRawShort();
        spline.setFlags(flags);

        // 제어점 개수
        int numControlPoints = r.getInput().readRawShort();
        for (int i = 0; i < numControlPoints; i++) {
            double[] pt = r.read3BitDouble();
            spline.addControlPoint(new Point3D(pt[0], pt[1], pt[2]));
        }

        // 핏점 개수
        int numFitPoints = r.getInput().readRawShort();
        for (int i = 0; i < numFitPoints; i++) {
            double[] pt = r.read3BitDouble();
            spline.addFitPoint(new Point3D(pt[0], pt[1], pt[2]));
        }
    }
}
