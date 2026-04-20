package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import structure.DwgVersion;
import structure.entities.DwgMLine;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.Point3D;

public class MLineObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.MLINE.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgMLine)) return;
        DwgMLine mline = (DwgMLine) target;

        int byteOff = offset;

        // Scale (BD)
        if (byteOff + 8 <= data.length) {
            double scale = readBitDouble(data, byteOff);
            mline.setScale(scale);
            byteOff += 8;
        }

        // Justification (BS)
        if (byteOff + 2 <= data.length) {
            int just = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            mline.setJustification(just);
            byteOff += 2;
        }

        // Extrusion (3 × BD)
        if (byteOff + 24 <= data.length) {
            double[] extr = new double[3];
            extr[0] = readBitDouble(data, byteOff);
            extr[1] = readBitDouble(data, byteOff + 8);
            extr[2] = readBitDouble(data, byteOff + 16);
            mline.setExtrusion(extr);
            byteOff += 24;
        }

        // Number of vertices (BS)
        int numVertices = 0;
        if (byteOff + 2 <= data.length) {
            short nv = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            numVertices = (int) (nv & 0xFFFF);
            byteOff += 2;
        }

        // Read vertices (each is 3 × BD)
        List<Point3D> vertices = new ArrayList<>();
        for (int i = 0; i < numVertices && byteOff + 24 <= data.length; i++) {
            double x = readBitDouble(data, byteOff);
            double y = readBitDouble(data, byteOff + 8);
            double z = readBitDouble(data, byteOff + 16);
            vertices.add(new Point3D(x, y, z));
            byteOff += 24;
        }
        for (Point3D v : vertices) {
            mline.addVertex(v);
        }

        // Style handle (H)
        if (byteOff + 2 <= data.length) {
            short handle = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            mline.setStyleHandle(String.valueOf(handle & 0xFFFF));
        }
    }

    private static double readBitDouble(byte[] data, int byteOff) {
        if (byteOff + 8 > data.length) return 0.0;
        long bits = ByteBuffer.wrap(data, byteOff, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        return Double.longBitsToDouble(bits);
    }
}
