package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.DwgSolid;
import structure.entities.Point3D;

public class SolidObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.SOLID.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgSolid)) return;
        DwgSolid solid = (DwgSolid) target;

        int byteOff = offset;
        Point3D[] points = new Point3D[4];

        // Read 4 corner points (each 3 × BD)
        for (int i = 0; i < 4 && byteOff + 24 <= data.length; i++) {
            double x = readBitDouble(data, byteOff);
            double y = readBitDouble(data, byteOff + 8);
            double z = readBitDouble(data, byteOff + 16);
            points[i] = new Point3D(x, y, z);
            byteOff += 24;
        }
        solid.setPoints(points);

        // Extrusion (3 × BD, optional)
        if (byteOff + 24 <= data.length) {
            double[] extr = new double[3];
            extr[0] = readBitDouble(data, byteOff);
            extr[1] = readBitDouble(data, byteOff + 8);
            extr[2] = readBitDouble(data, byteOff + 16);
            solid.setExtrusion(extr);
        }
    }

    private static double readBitDouble(byte[] data, int byteOff) {
        if (byteOff + 8 > data.length) return 0.0;
        long bits = ByteBuffer.wrap(data, byteOff, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        return Double.longBitsToDouble(bits);
    }
}
