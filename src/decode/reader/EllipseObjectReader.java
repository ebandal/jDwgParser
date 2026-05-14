package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgEllipse;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.Point3D;

public class EllipseObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.ELLIPSE.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgEllipse)) return;
        DwgEllipse ellipse = (DwgEllipse) target;

        // Log hex dump of Ellipse data
        java.util.logging.Logger log = java.util.logging.Logger.getLogger(getClass().getName());
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < Math.min(32, data.length); i++) {
            hex.append(String.format("%02x ", data[i] & 0xFF));
        }
        log.info("Ellipse data (" + data.length + " bytes): " + hex.toString());

        int byteOff = offset;

        // Center (3 × BD)
        double cx = readBitDouble(data, byteOff);
        byteOff += 8;
        double cy = readBitDouble(data, byteOff);
        byteOff += 8;
        double cz = readBitDouble(data, byteOff);
        byteOff += 8;
        ellipse.setCenter(new Point3D(cx, cy, cz));

        // Major axis vector (3 × BD)
        double mx = readBitDouble(data, byteOff);
        byteOff += 8;
        double my = readBitDouble(data, byteOff);
        byteOff += 8;
        double mz = readBitDouble(data, byteOff);
        byteOff += 8;
        ellipse.setMajorAxisVec(new Point3D(mx, my, mz));

        // Axis ratio (BD)
        if (byteOff + 8 <= data.length) {
            double ratio = readBitDouble(data, byteOff);
            ellipse.setAxisRatio(ratio);
            byteOff += 8;
        }

        // Start parameter (BD)
        if (byteOff + 8 <= data.length) {
            double start = readBitDouble(data, byteOff);
            ellipse.setStartParam(start);
            byteOff += 8;
        }

        // End parameter (BD)
        if (byteOff + 8 <= data.length) {
            double end = readBitDouble(data, byteOff);
            ellipse.setEndParam(end);
            byteOff += 8;
        }

        // Extrusion (3 × BD)
        if (byteOff + 24 <= data.length) {
            double[] extr = new double[3];
            extr[0] = readBitDouble(data, byteOff);
            extr[1] = readBitDouble(data, byteOff + 8);
            extr[2] = readBitDouble(data, byteOff + 16);
            ellipse.setExtrusion(extr);
        }
    }

    private static double readBitDouble(byte[] data, int byteOff) {
        if (byteOff + 8 > data.length) return 0.0;
        long bits = ByteBuffer.wrap(data, byteOff, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        return Double.longBitsToDouble(bits);
    }
}
