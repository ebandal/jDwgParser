package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.DwgView;
import structure.entities.Point3D;

public class ViewObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.VIEW.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgView)) return;
        DwgView view = (DwgView) target;

        int byteOff = offset;

        // Name (T)
        if (byteOff + 2 <= data.length) {
            String name = readText(data, byteOff);
            view.setName(name);
            byteOff += 2 + name.length();
        }

        // View target (3 × BD)
        if (byteOff + 24 <= data.length) {
            double tx = readBitDouble(data, byteOff);
            double ty = readBitDouble(data, byteOff + 8);
            double tz = readBitDouble(data, byteOff + 16);
            view.setViewTarget(new Point3D(tx, ty, tz));
            byteOff += 24;
        }

        // View direction (3 × BD)
        if (byteOff + 24 <= data.length) {
            double dx = readBitDouble(data, byteOff);
            double dy = readBitDouble(data, byteOff + 8);
            double dz = readBitDouble(data, byteOff + 16);
            view.setViewDirection(new Point3D(dx, dy, dz));
            byteOff += 24;
        }

        // View height (BD)
        if (byteOff + 8 <= data.length) {
            double height = readBitDouble(data, byteOff);
            view.setViewHeight(height);
            byteOff += 8;
        }

        // View width (BD)
        if (byteOff + 8 <= data.length) {
            double width = readBitDouble(data, byteOff);
            view.setViewWidth(width);
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
