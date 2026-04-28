package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgSurface;
import io.dwg.sections.objects.ObjectReader;

public class SurfaceObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.SURFACE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgSurface surface = (DwgSurface) target;

        // Surface type
        surface.setType(r.readBitShort());

        // Degree in U and V directions
        surface.setDegreeU(r.readBitShort());
        surface.setDegreeV(r.readBitShort());

        // Number of control points
        int numU = r.readBitShort();
        int numV = r.readBitShort();
        surface.setNumControlPointsU(numU);
        surface.setNumControlPointsV(numV);

        // Number of knots
        int knotsU = r.readBitShort();
        int knotsV = r.readBitShort();
        surface.setNumKnotsU(knotsU);
        surface.setNumKnotsV(knotsV);

        // Read knot vectors
        double[] knotsUArray = new double[knotsU];
        for (int i = 0; i < knotsU; i++) {
            knotsUArray[i] = r.readBitDouble();
        }
        surface.setKnotsU(knotsUArray);

        double[] knotsVArray = new double[knotsV];
        for (int i = 0; i < knotsV; i++) {
            knotsVArray[i] = r.readBitDouble();
        }
        surface.setKnotsV(knotsVArray);

        // Read control points with weights
        double[][] controlPoints = new double[numU * numV][3];
        double[] weights = new double[numU * numV];

        for (int i = 0; i < numU * numV; i++) {
            double[] point = r.read3BitDouble();
            controlPoints[i][0] = point[0];
            controlPoints[i][1] = point[1];
            controlPoints[i][2] = point[2];
            weights[i] = r.readBitDouble();
        }

        surface.setControlPoints(controlPoints);
        surface.setWeights(weights);
    }
}
