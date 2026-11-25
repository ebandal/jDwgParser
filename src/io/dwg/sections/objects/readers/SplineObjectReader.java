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
 * SPLINE (type 36), spec §20.4.40.
 * <pre>
 *   scenario BL: 1 control points and knots, 2 fit points
 *   R2013+   spline flags 1 BL, knot parameter BL: the scenario becomes 2 when the spline is
 *            defined by fit points (flag 1) and 1 when the knot parameter is Custom (15)
 *   degree BL
 *   scenario 1: rational B, closed B, periodic B, knot tolerance BD, control tolerance BD,
 *               number of knots BL, number of control points BL, weights present B,
 *               knots BD each, control points 3BD each (followed by a weight BD if present)
 *   scenario 2: fit tolerance BD, start tangent 3BD, end tangent 3BD, number of fit points BL,
 *               fit points 3BD each
 * </pre>
 */
public class SplineObjectReader implements ObjectReader {

    private static final int METHOD_FIT_POINTS = 1;
    private static final int KNOT_PARAMETER_CUSTOM = 15;

    @Override
    public int objectType() { return DwgObjectType.SPLINE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);
        DwgSpline spline = (DwgSpline) target;

        int scenario = r.readBitLong();
        if (v.from(DwgVersion.R2013)) {
            int flags = r.readBitLong();
            int knotParameter = r.readBitLong();
            spline.setFlags(flags);
            if ((flags & METHOD_FIT_POINTS) != 0) scenario = 2;
            if (knotParameter == KNOT_PARAMETER_CUSTOM) scenario = 1;
        }
        spline.setDegree(r.readBitLong());

        if ((scenario & 1) != 0) {
            readControlPoints(spline, r);
        } else {
            readFitPoints(spline, r);
        }
    }

    private static void readControlPoints(DwgSpline spline, BitStreamReader r) {
        spline.setRational(r.getInput().readBit());
        spline.setClosed(r.getInput().readBit());
        spline.setPeriodic(r.getInput().readBit());
        r.readBitDouble();                               // knot tolerance
        r.readBitDouble();                               // control point tolerance
        int knotCount = r.readBitLong();
        int controlPointCount = r.readBitLong();
        boolean weighted = r.getInput().readBit();
        for (int i = 0; i < knotCount; i++) {
            spline.knots().add(r.readBitDouble());
        }
        for (int i = 0; i < controlPointCount; i++) {
            spline.addControlPoint(point(r.read3BitDouble()));
            if (weighted) {
                spline.weights().add(r.readBitDouble());
            }
        }
    }

    private static void readFitPoints(DwgSpline spline, BitStreamReader r) {
        r.readBitDouble();                               // fit tolerance
        r.read3BitDouble();                              // start tangent
        r.read3BitDouble();                              // end tangent
        int fitPointCount = r.readBitLong();
        for (int i = 0; i < fitPointCount; i++) {
            spline.addFitPoint(point(r.read3BitDouble()));
        }
    }

    private static Point3D point(double[] p) {
        return new Point3D(p[0], p[1], p[2]);
    }
}
