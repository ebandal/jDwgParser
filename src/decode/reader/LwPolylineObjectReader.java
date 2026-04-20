package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import structure.DwgVersion;
import structure.entities.DwgLwPolyline;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.Point2D;

public class LwPolylineObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.LWPLINE.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgLwPolyline)) return;
        DwgLwPolyline lwpline = (DwgLwPolyline) target;

        int byteOff = offset;

        // Flags (BS)
        if (byteOff + 2 <= data.length) {
            int flags = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            lwpline.setFlags(flags);
            byteOff += 2;
        }

        // Constant width (BD)
        if (byteOff + 8 <= data.length) {
            double cw = readBitDouble(data, byteOff);
            lwpline.setConstantWidth(cw);
            byteOff += 8;
        }

        // Elevation (BD)
        if (byteOff + 8 <= data.length) {
            double elev = readBitDouble(data, byteOff);
            lwpline.setElevation(elev);
            byteOff += 8;
        }

        // Thickness (BD)
        if (byteOff + 8 <= data.length) {
            double thick = readBitDouble(data, byteOff);
            lwpline.setThickness(thick);
            byteOff += 8;
        }

        // Extrusion (3 × BD, optional)
        if (byteOff + 24 <= data.length) {
            double[] extr = new double[3];
            extr[0] = readBitDouble(data, byteOff);
            extr[1] = readBitDouble(data, byteOff + 8);
            extr[2] = readBitDouble(data, byteOff + 16);
            lwpline.setExtrusion(extr);
            byteOff += 24;
        }

        // Number of vertices (BS)
        int numVertices = 0;
        if (byteOff + 2 <= data.length) {
            short nv = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            numVertices = (int) (nv & 0xFFFF);
            byteOff += 2;
        }

        // Read vertices (each is 2 × RD)
        List<Point2D> vertices = new ArrayList<>();
        for (int i = 0; i < numVertices && byteOff + 16 <= data.length; i++) {
            double x = readBitDouble(data, byteOff);
            double y = readBitDouble(data, byteOff + 8);
            vertices.add(new Point2D(x, y));
            byteOff += 16;
        }
        lwpline.setVertices(vertices);
    }

    private static double readBitDouble(byte[] data, int byteOff) {
        if (byteOff + 8 > data.length) return 0.0;
        long bits = ByteBuffer.wrap(data, byteOff, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        return Double.longBitsToDouble(bits);
    }
}
