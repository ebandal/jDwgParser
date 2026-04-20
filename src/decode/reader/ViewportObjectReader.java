package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.DwgViewport;
import structure.entities.Point3D;

public class ViewportObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.VIEWPORT.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgViewport)) return;
        DwgViewport viewport = (DwgViewport) target;

        int byteOff = offset;

        // Center (3 × BD)
        double cx = readBitDouble(data, byteOff);
        byteOff += 8;
        double cy = readBitDouble(data, byteOff);
        byteOff += 8;
        double cz = readBitDouble(data, byteOff);
        byteOff += 8;
        viewport.setCenter(new Point3D(cx, cy, cz));

        // Width (BD)
        if (byteOff + 8 <= data.length) {
            double width = readBitDouble(data, byteOff);
            viewport.setWidth(width);
            byteOff += 8;
        }

        // Height (BD)
        if (byteOff + 8 <= data.length) {
            double height = readBitDouble(data, byteOff);
            viewport.setHeight(height);
            byteOff += 8;
        }

        // View height (BD)
        if (byteOff + 8 <= data.length) {
            double viewHeight = readBitDouble(data, byteOff);
            viewport.setViewHeight(viewHeight);
            byteOff += 8;
        }

        // Extrusion (3 × BD, optional)
        if (byteOff + 24 <= data.length) {
            double[] extr = new double[3];
            extr[0] = readBitDouble(data, byteOff);
            extr[1] = readBitDouble(data, byteOff + 8);
            extr[2] = readBitDouble(data, byteOff + 16);
            viewport.setExtrusion(extr);
        }
    }

    private static double readBitDouble(byte[] data, int byteOff) {
        if (byteOff + 8 > data.length) return 0.0;
        long bits = ByteBuffer.wrap(data, byteOff, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        return Double.longBitsToDouble(bits);
    }
}
