package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import structure.DwgVersion;
import structure.entities.DwgLeader;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.Point3D;

public class LeaderObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.LEADER.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgLeader)) return;
        DwgLeader leader = (DwgLeader) target;

        int byteOff = offset;

        // Style name (T)
        if (byteOff + 2 <= data.length) {
            String style = readText(data, byteOff);
            leader.setStyleName(style);
            byteOff += 2 + style.length();
        }

        // Arrow type (BS)
        if (byteOff + 2 <= data.length) {
            int arrow = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            leader.setArrow(arrow);
            byteOff += 2;
        }

        // Number of leader points (BS)
        int numPoints = 0;
        if (byteOff + 2 <= data.length) {
            short np = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            numPoints = (int) (np & 0xFFFF);
            byteOff += 2;
        }

        // Read leader points (each is 3 × BD)
        List<Point3D> points = new ArrayList<>();
        for (int i = 0; i < numPoints && byteOff + 24 <= data.length; i++) {
            double x = readBitDouble(data, byteOff);
            double y = readBitDouble(data, byteOff + 8);
            double z = readBitDouble(data, byteOff + 16);
            points.add(new Point3D(x, y, z));
            byteOff += 24;
        }
        leader.setPoints(points);

        // Extrusion (3 × BD, optional)
        if (byteOff + 24 <= data.length) {
            double[] extr = new double[3];
            extr[0] = readBitDouble(data, byteOff);
            extr[1] = readBitDouble(data, byteOff + 8);
            extr[2] = readBitDouble(data, byteOff + 16);
            leader.setExtrusion(extr);
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
