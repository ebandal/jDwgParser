package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.DwgVport;
import structure.entities.Point3D;

public class VportObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.VPORT.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgVport)) return;
        DwgVport vport = (DwgVport) target;

        int byteOff = offset;

        // Name (T)
        if (byteOff + 2 <= data.length) {
            String name = readText(data, byteOff);
            vport.setName(name);
            byteOff += 2 + name.length();
        }

        // View center (2 × BD)
        if (byteOff + 16 <= data.length) {
            double cx = readBitDouble(data, byteOff);
            double cy = readBitDouble(data, byteOff + 8);
            vport.setViewCenter(new Point3D(cx, cy, 0.0));
            byteOff += 16;
        }

        // Snap base (2 × BD)
        if (byteOff + 16 <= data.length) {
            double sx = readBitDouble(data, byteOff);
            double sy = readBitDouble(data, byteOff + 8);
            vport.setSnapBase(new Point3D(sx, sy, 0.0));
            byteOff += 16;
        }

        // Grid spacing X (BD)
        if (byteOff + 8 <= data.length) {
            double gridX = readBitDouble(data, byteOff);
            vport.setGridSpacingX(gridX);
            byteOff += 8;
        }

        // Grid spacing Y (BD)
        if (byteOff + 8 <= data.length) {
            double gridY = readBitDouble(data, byteOff);
            vport.setGridSpacingY(gridY);
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
