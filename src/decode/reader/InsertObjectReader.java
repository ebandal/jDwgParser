package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgInsert;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.Point3D;

public class InsertObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.INSERT.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgInsert)) return;
        DwgInsert insert = (DwgInsert) target;

        int byteOff = offset;

        // Insertion point (3 × BD)
        double ix = readBitDouble(data, byteOff);
        byteOff += 8;
        double iy = readBitDouble(data, byteOff);
        byteOff += 8;
        double iz = readBitDouble(data, byteOff);
        byteOff += 8;
        insert.setInsertionPoint(new Point3D(ix, iy, iz));

        // Scale factors (3 × BD)
        double xs = readBitDouble(data, byteOff);
        byteOff += 8;
        double ys = readBitDouble(data, byteOff);
        byteOff += 8;
        double zs = readBitDouble(data, byteOff);
        byteOff += 8;
        insert.setXScale(xs);
        insert.setYScale(ys);
        insert.setZScale(zs);

        // Rotation angle (BD)
        if (byteOff + 8 <= data.length) {
            double rot = readBitDouble(data, byteOff);
            insert.setRotation(rot);
            byteOff += 8;
        }

        // Extrusion (3 × BD)
        if (byteOff + 24 <= data.length) {
            double[] extr = new double[3];
            extr[0] = readBitDouble(data, byteOff);
            extr[1] = readBitDouble(data, byteOff + 8);
            extr[2] = readBitDouble(data, byteOff + 16);
            insert.setExtrusion(extr);
            byteOff += 24;
        }

        // Has attributes flag (B)
        if (byteOff < data.length) {
            boolean hasAttribs = readBit(data, byteOff, 0);
            insert.setHasAttribs(hasAttribs);
        }
    }

    private static boolean readBit(byte[] data, int byteOff, int bitOff) {
        if (byteOff >= data.length) return false;
        return ((data[byteOff] >> (7 - bitOff)) & 1) == 1;
    }

    private static double readBitDouble(byte[] data, int byteOff) {
        if (byteOff + 8 > data.length) return 0.0;
        long bits = ByteBuffer.wrap(data, byteOff, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        return Double.longBitsToDouble(bits);
    }
}
