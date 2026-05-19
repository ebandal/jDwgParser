package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgSpline;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * SPLINE 엔티티 리더 (타입 0x24)
 * libredwg dwg.spec DWG_ENTITY(SPLINE)
 *
 * scenario=1: rational B-spline (knots + control points)
 * scenario=2: bezier spline (fit points only)
 */
public class SplineObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.SPLINE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);

        DwgSpline spline = (DwgSpline) target;

        int scenario = r.readBitLong();
        if (v.from(DwgVersion.R2013)) {
            int splineFlags = r.readBitLong();
            int knotParam   = r.readBitLong();
            spline.setFlags(splineFlags);
            if ((splineFlags & 1) != 0)  scenario = 2;
            if (knotParam == 15)          scenario = 1;
        }

        int degree = r.readBitLong();
        spline.setDegree(degree);

        if ((scenario & 1) != 0) {
            // scenario 1: rational B-spline
            r.getInput().readBit(); // rational
            r.getInput().readBit(); // closed_b
            r.getInput().readBit(); // periodic
            r.readBitDouble(); // knot_tol
            r.readBitDouble(); // ctrl_tol
            int numKnots    = r.readBitLong();
            int numCtrlPts  = r.readBitLong();
            boolean weighted = r.getInput().readBit();

            // knots
            for (int i = 0; i < numKnots; i++) {
                r.readBitDouble();
            }

            // control points
            for (int i = 0; i < numCtrlPts; i++) {
                double[] pt = r.read3BitDouble();
                spline.addControlPoint(new Point3D(pt[0], pt[1], pt[2]));
                if (weighted) r.readBitDouble(); // weight
            }
        } else {
            // scenario 2: bezier spline
            r.readBitDouble(); // fit_tol
            r.read3BitDouble(); // beg_tan_vec
            r.read3BitDouble(); // end_tan_vec
            int numFitPts = r.readBitLong();
            for (int i = 0; i < numFitPts; i++) {
                double[] pt = r.read3BitDouble();
                spline.addFitPoint(new Point3D(pt[0], pt[1], pt[2]));
            }
        }
    }
}
