package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.DwgShape;
import structure.entities.Point3D;

public class ShapeObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.SHAPE.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgShape)) return;
        DwgShape shape = (DwgShape) target;

        int byteOff = offset;

        // Insertion point (3 × BD)
        double x = readBitDouble(data, byteOff);
        byteOff += 8;
        double y = readBitDouble(data, byteOff);
        byteOff += 8;
        double z = readBitDouble(data, byteOff);
        byteOff += 8;
        shape.setInsertionPoint(new Point3D(x, y, z));

        // Scale (BD)
        if (byteOff + 8 <= data.length) {
            double scale = readBitDouble(data, byteOff);
            shape.setScale(scale);
            byteOff += 8;
        }

        // Rotation (BD)
        if (byteOff + 8 <= data.length) {
            double rotation = readBitDouble(data, byteOff);
            shape.setAngle(rotation);
            byteOff += 8;
        }

        // Extrusion (3 × BD, optional)
        if (byteOff + 24 <= data.length) {
            double[] extr = new double[3];
            extr[0] = readBitDouble(data, byteOff);
            extr[1] = readBitDouble(data, byteOff + 8);
            extr[2] = readBitDouble(data, byteOff + 16);
            shape.setExtrusion(extr);
            byteOff += 24;
        }

        // Shape name (T, optional)
        if (byteOff + 2 <= data.length) {
            String name = readText(data, byteOff);
            shape.setShapeName(name);
        }
    }

    private static double readBitDouble(byte[] data, int byteOff) {
        if (byteOff + 8 > data.length) return 0.0;
        long bits = ByteBuffer.wrap(data, byteOff, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        return Double.longBitsToDouble(bits);
    }

    private static String readText(byte[] data, int byteOff) {
        if (byteOff + 2 > data.length) return "";
        short len = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        if (len <= 0 || byteOff + 2 + len > data.length) return "";
        return new String(data, byteOff + 2, len);
    }
}
