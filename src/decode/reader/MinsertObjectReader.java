package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgMinsert;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.Point3D;

public class MinsertObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.MINSERT.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgMinsert)) return;
        DwgMinsert minsert = (DwgMinsert) target;

        int byteOff = offset;

        // Insertion point (3 × BD)
        double x = readBitDouble(data, byteOff);
        byteOff += 8;
        double y = readBitDouble(data, byteOff);
        byteOff += 8;
        double z = readBitDouble(data, byteOff);
        byteOff += 8;
        minsert.setInsertionPoint(new Point3D(x, y, z));

        // Scale X, Y, Z (3 × BD)
        if (byteOff + 24 <= data.length) {
            double[] scale = new double[3];
            scale[0] = readBitDouble(data, byteOff);
            scale[1] = readBitDouble(data, byteOff + 8);
            scale[2] = readBitDouble(data, byteOff + 16);
            minsert.setScale(scale);
            byteOff += 24;
        }

        // Rotation (BD)
        if (byteOff + 8 <= data.length) {
            double rotation = readBitDouble(data, byteOff);
            minsert.setRotation(rotation);
            byteOff += 8;
        }

        // Extrusion (3 × BD, optional)
        if (byteOff + 24 <= data.length) {
            double[] extr = new double[3];
            extr[0] = readBitDouble(data, byteOff);
            extr[1] = readBitDouble(data, byteOff + 8);
            extr[2] = readBitDouble(data, byteOff + 16);
            minsert.setExtrusion(extr);
            byteOff += 24;
        }

        // Row count (BS)
        if (byteOff + 2 <= data.length) {
            short rc = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            minsert.setRowCount((int) (rc & 0xFFFF));
            byteOff += 2;
        }

        // Column count (BS)
        if (byteOff + 2 <= data.length) {
            short cc = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            minsert.setColumnCount((int) (cc & 0xFFFF));
            byteOff += 2;
        }

        // Row spacing (BD)
        if (byteOff + 8 <= data.length) {
            double rs = readBitDouble(data, byteOff);
            minsert.setRowSpacing(rs);
            byteOff += 8;
        }

        // Column spacing (BD)
        if (byteOff + 8 <= data.length) {
            double cs = readBitDouble(data, byteOff);
            minsert.setColumnSpacing(cs);
        }
    }

    private static double readBitDouble(byte[] data, int byteOff) {
        if (byteOff + 8 > data.length) return 0.0;
        long bits = ByteBuffer.wrap(data, byteOff, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        return Double.longBitsToDouble(bits);
    }
}
