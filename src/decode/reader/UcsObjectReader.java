package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.DwgUcs;
import structure.entities.Point3D;

public class UcsObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.UCS.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgUcs)) return;
        DwgUcs ucs = (DwgUcs) target;

        int byteOff = offset;

        // Name (T)
        if (byteOff + 2 <= data.length) {
            String name = readText(data, byteOff);
            ucs.setName(name);
            byteOff += 2 + name.length();
        }

        // Origin (3 × BD)
        if (byteOff + 24 <= data.length) {
            double ox = readBitDouble(data, byteOff);
            double oy = readBitDouble(data, byteOff + 8);
            double oz = readBitDouble(data, byteOff + 16);
            ucs.setOrigin(new Point3D(ox, oy, oz));
            byteOff += 24;
        }

        // X direction (3 × BD)
        if (byteOff + 24 <= data.length) {
            double xx = readBitDouble(data, byteOff);
            double xy = readBitDouble(data, byteOff + 8);
            double xz = readBitDouble(data, byteOff + 16);
            ucs.setXDirection(new Point3D(xx, xy, xz));
            byteOff += 24;
        }

        // Y direction (3 × BD)
        if (byteOff + 24 <= data.length) {
            double yx = readBitDouble(data, byteOff);
            double yy = readBitDouble(data, byteOff + 8);
            double yz = readBitDouble(data, byteOff + 16);
            ucs.setYDirection(new Point3D(yx, yy, yz));
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
