package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.DwgPolyline2D;
import structure.entities.Point3D;

public class PolylineObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.POLYLINE_2D.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgPolyline2D)) return;
        DwgPolyline2D pline = (DwgPolyline2D) target;

        int byteOff = offset;

        // Flags (BS)
        if (byteOff + 2 <= data.length) {
            int flags = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            pline.setFlags(flags);
            byteOff += 2;
        }

        // Curve type (BS)
        if (byteOff + 2 <= data.length) {
            byteOff += 2; // skip curve type for now
        }

        // Default start width (BD)
        if (byteOff + 8 <= data.length) {
            double sw = readBitDouble(data, byteOff);
            pline.setDefaultStartWidth(sw);
            byteOff += 8;
        }

        // Default end width (BD)
        if (byteOff + 8 <= data.length) {
            double ew = readBitDouble(data, byteOff);
            pline.setDefaultEndWidth(ew);
            byteOff += 8;
        }

        // Elevation (BD)
        if (byteOff + 8 <= data.length) {
            double elev = readBitDouble(data, byteOff);
            pline.setElevation(elev);
            byteOff += 8;
        }

        // Extrusion (3 × BD, optional)
        if (byteOff + 24 <= data.length) {
            double[] extr = new double[3];
            extr[0] = readBitDouble(data, byteOff);
            extr[1] = readBitDouble(data, byteOff + 8);
            extr[2] = readBitDouble(data, byteOff + 16);
            pline.setExtrusion(extr);
        }
    }

    private static double readBitDouble(byte[] data, int byteOff) {
        if (byteOff + 8 > data.length) return 0.0;
        long bits = ByteBuffer.wrap(data, byteOff, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        return Double.longBitsToDouble(bits);
    }
}
