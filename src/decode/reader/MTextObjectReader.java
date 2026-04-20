package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgMText;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.Point3D;

public class MTextObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.MTEXT.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgMText)) return;
        DwgMText mtext = (DwgMText) target;

        int byteOff = offset;

        // Insertion point (3 × BD)
        double x = readBitDouble(data, byteOff);
        byteOff += 8;
        double y = readBitDouble(data, byteOff);
        byteOff += 8;
        double z = readBitDouble(data, byteOff);
        byteOff += 8;
        mtext.setLocation(new Point3D(x, y, z));

        // Extrusion (3 × BD)
        if (byteOff + 24 <= data.length) {
            double[] extr = new double[3];
            extr[0] = readBitDouble(data, byteOff);
            extr[1] = readBitDouble(data, byteOff + 8);
            extr[2] = readBitDouble(data, byteOff + 16);
            mtext.setExtrusion(extr);
            byteOff += 24;
        }

        // Text height (BD)
        if (byteOff + 8 <= data.length) {
            double height = readBitDouble(data, byteOff);
            mtext.setHeight(height);
            byteOff += 8;
        }

        // Width (BD)
        if (byteOff + 8 <= data.length) {
            double width = readBitDouble(data, byteOff);
            mtext.setWidth(width);
            byteOff += 8;
        }

        // Attachment point (BS)
        if (byteOff + 2 <= data.length) {
            short ap = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            mtext.setAttachmentPoint((int) (ap & 0xFFFF));
            byteOff += 2;
        }

        // Drawing direction (BS)
        if (byteOff + 2 <= data.length) {
            byteOff += 2; // skip drawing direction
        }

        // Text string (T)
        if (byteOff + 2 <= data.length) {
            String text = readText(data, byteOff);
            mtext.setText(text);
            byteOff += 2 + text.length();
        }

        // Text rotation (BD)
        if (byteOff + 8 <= data.length) {
            double rotation = readBitDouble(data, byteOff);
            mtext.setAngle(rotation);
            byteOff += 8;
        }

        // Style name (T, optional)
        if (byteOff + 2 <= data.length) {
            String style = readText(data, byteOff);
            mtext.setStyleName(style);
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
