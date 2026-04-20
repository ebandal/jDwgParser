package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import structure.DwgVersion;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.DwgSpline;
import structure.entities.Point3D;

public class SplineObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.SPLINE.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgSpline)) return;
        DwgSpline spline = (DwgSpline) target;

        int byteOff = offset;

        // Flags (BS)
        if (byteOff + 2 <= data.length) {
            int flags = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            spline.setFlags(flags);
            byteOff += 2;
        }

        // Degree (BS)
        if (byteOff + 2 <= data.length) {
            int degree = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            spline.setDegree(degree);
            byteOff += 2;
        }

        // Fit tolerance (BD)
        if (byteOff + 8 <= data.length) {
            byteOff += 8; // skip fit tolerance
        }

        // Control point tolerance (BD)
        if (byteOff + 8 <= data.length) {
            byteOff += 8; // skip control point tolerance
        }

        // Knot tolerance (BD)
        if (byteOff + 8 <= data.length) {
            byteOff += 8; // skip knot tolerance
        }

        // Extrusion (3 × BD)
        if (byteOff + 24 <= data.length) {
            double[] extr = new double[3];
            extr[0] = readBitDouble(data, byteOff);
            extr[1] = readBitDouble(data, byteOff + 8);
            extr[2] = readBitDouble(data, byteOff + 16);
            spline.setExtrusion(extr);
            byteOff += 24;
        }

        // Number of control points (BS)
        int numControlPoints = 0;
        if (byteOff + 2 <= data.length) {
            short ncp = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            numControlPoints = (int) (ncp & 0xFFFF);
            byteOff += 2;
        }

        // Read control points (each is 3 × BD)
        List<Point3D> controlPoints = new ArrayList<>();
        for (int i = 0; i < numControlPoints && byteOff + 24 <= data.length; i++) {
            double x = readBitDouble(data, byteOff);
            double y = readBitDouble(data, byteOff + 8);
            double z = readBitDouble(data, byteOff + 16);
            controlPoints.add(new Point3D(x, y, z));
            byteOff += 24;
        }
        spline.setControlPoints(controlPoints);

        // Number of fit points (BS)
        int numFitPoints = 0;
        if (byteOff + 2 <= data.length) {
            short nfp = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            numFitPoints = (int) (nfp & 0xFFFF);
            byteOff += 2;
        }

        // Read fit points (each is 3 × BD)
        List<Point3D> fitPoints = new ArrayList<>();
        for (int i = 0; i < numFitPoints && byteOff + 24 <= data.length; i++) {
            double x = readBitDouble(data, byteOff);
            double y = readBitDouble(data, byteOff + 8);
            double z = readBitDouble(data, byteOff + 16);
            fitPoints.add(new Point3D(x, y, z));
            byteOff += 24;
        }
        spline.setFitPoints(fitPoints);
    }

    private static double readBitDouble(byte[] data, int byteOff) {
        if (byteOff + 8 > data.length) return 0.0;
        long bits = ByteBuffer.wrap(data, byteOff, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        return Double.longBitsToDouble(bits);
    }
}
