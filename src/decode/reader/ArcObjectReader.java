package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgArc;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.Point3D;

public class ArcObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.ARC.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgArc)) return;
        DwgArc arc = (DwgArc) target;

        int byteOff = offset;

        // Center (3 × BD)
        double cx = readBitDouble(data, byteOff);
        byteOff += 8;
        double cy = readBitDouble(data, byteOff);
        byteOff += 8;
        double cz = readBitDouble(data, byteOff);
        byteOff += 8;
        arc.setCenter(new Point3D(cx, cy, cz));

        // Radius (BD)
        double radius = readBitDouble(data, byteOff);
        byteOff += 8;
        arc.setRadius(radius);

        // Start angle (BD)
        double startAngle = readBitDouble(data, byteOff);
        byteOff += 8;
        arc.setStartAngle(startAngle);

        // End angle (BD)
        double endAngle = readBitDouble(data, byteOff);
        byteOff += 8;
        arc.setEndAngle(endAngle);

        // Thickness (BD, optional)
        if (byteOff < data.length) {
            double thickness = readBitDouble(data, byteOff);
            byteOff += 8;
            arc.setThickness(thickness);
        }

        // Extrusion (3 × BD, optional)
        if (byteOff + 24 <= data.length) {
            double[] extr = new double[3];
            extr[0] = readBitDouble(data, byteOff);
            extr[1] = readBitDouble(data, byteOff + 8);
            extr[2] = readBitDouble(data, byteOff + 16);
            arc.setExtrusion(extr);
        }
    }

    private static double readBitDouble(byte[] data, int byteOff) {
        if (byteOff + 8 > data.length) return 0.0;
        long bits = ByteBuffer.wrap(data, byteOff, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        return Double.longBitsToDouble(bits);
    }
}
