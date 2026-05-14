package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.DwgTolerance;
import structure.entities.Point3D;

public class ToleranceObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.TOLERANCE.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgTolerance)) return;
        DwgTolerance tolerance = (DwgTolerance) target;

        int byteOff = offset;

        // Insertion point (3 × BD)
        double x = readBitDouble(data, byteOff);
        byteOff += 8;
        double y = readBitDouble(data, byteOff);
        byteOff += 8;
        double z = readBitDouble(data, byteOff);
        byteOff += 8;
        tolerance.setInsertionPoint(new Point3D(x, y, z));

        // Direction (3 × BD)
        if (byteOff + 24 <= data.length) {
            double[] direction = new double[3];
            direction[0] = readBitDouble(data, byteOff);
            direction[1] = readBitDouble(data, byteOff + 8);
            direction[2] = readBitDouble(data, byteOff + 16);
            tolerance.setDirection(direction);
            byteOff += 24;
        }

        // Dimension style name (T)
        if (byteOff + 2 <= data.length) {
            String dimStyle = readText(data, byteOff);
            tolerance.setDimensionStyleName(dimStyle);
            byteOff += 2 + dimStyle.length();
        }

        // Tolerance text (T)
        if (byteOff + 2 <= data.length) {
            String text = readText(data, byteOff);
            tolerance.setToleranceText(text);
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
