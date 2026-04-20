package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.DwgXLine;
import structure.entities.Point3D;

public class XLineObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.XLINE.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgXLine)) return;
        DwgXLine xline = (DwgXLine) target;

        int byteOff = offset;

        // Start point (3 × BD)
        double sx = readBitDouble(data, byteOff);
        byteOff += 8;
        double sy = readBitDouble(data, byteOff);
        byteOff += 8;
        double sz = readBitDouble(data, byteOff);
        byteOff += 8;
        xline.setStart(new Point3D(sx, sy, sz));

        // Direction vector (3 × BD)
        if (byteOff + 24 <= data.length) {
            double dx = readBitDouble(data, byteOff);
            double dy = readBitDouble(data, byteOff + 8);
            double dz = readBitDouble(data, byteOff + 16);
            xline.setDirection(new Point3D(dx, dy, dz));
        }
    }

    private static double readBitDouble(byte[] data, int byteOff) {
        if (byteOff + 8 > data.length) return 0.0;
        long bits = ByteBuffer.wrap(data, byteOff, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        return Double.longBitsToDouble(bits);
    }
}
