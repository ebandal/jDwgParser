package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.DwgText;
import structure.entities.Point2D;

public class TextObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.TEXT.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgText)) return;
        DwgText text = (DwgText) target;

        int byteOff = offset;

        // Data flags (B)
        boolean hasDataFlags = readBit(data, byteOff, 0);
        if (hasDataFlags) {
            double dataFlags = readBitDouble(data, byteOff + 1);
            byteOff += 9;
            text.setDataFlags(dataFlags);
        } else {
            byteOff += 1;
        }

        // Insertion point X, Y (2 × BD)
        double ix = readBitDouble(data, byteOff);
        byteOff += 8;
        double iy = readBitDouble(data, byteOff);
        byteOff += 8;
        text.setInsertionPoint(new Point2D(ix, iy));

        // Alignment point X, Y (2 × BD, optional)
        if (byteOff + 16 <= data.length) {
            double ax = readBitDouble(data, byteOff);
            double ay = readBitDouble(data, byteOff + 8);
            text.setAlignmentPoint(new Point2D(ax, ay));
            byteOff += 16;
        }

        // Extrusion (3 × BD)
        if (byteOff + 24 <= data.length) {
            double[] extr = new double[3];
            extr[0] = readBitDouble(data, byteOff);
            extr[1] = readBitDouble(data, byteOff + 8);
            extr[2] = readBitDouble(data, byteOff + 16);
            text.setExtrusion(extr);
            byteOff += 24;
        }

        // Height (BD)
        if (byteOff + 8 <= data.length) {
            double height = readBitDouble(data, byteOff);
            text.setHeight(height);
            byteOff += 8;
        }

        // Width factor (BD, optional)
        if (byteOff + 8 <= data.length) {
            double wf = readBitDouble(data, byteOff);
            if (wf != 0.0) {
                text.setWidthFactor(wf);
            }
            byteOff += 8;
        }

        // Rotation angle (BD, optional)
        if (byteOff + 8 <= data.length) {
            double rot = readBitDouble(data, byteOff);
            text.setRotationAngle(rot);
            byteOff += 8;
        }

        // Text value (T - text string)
        if (byteOff < data.length) {
            String value = readText(data, byteOff);
            text.setValue(value);
        }
    }

    private static boolean readBit(byte[] data, int byteOff, int bitOff) {
        if (byteOff >= data.length) return false;
        return ((data[byteOff] >> (7 - bitOff)) & 1) == 1;
    }

    private static double readBitDouble(byte[] data, int byteOff) {
        if (byteOff + 8 > data.length) return 0.0;
        long bits = ByteBuffer.wrap(data, byteOff, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        return Double.longBitsToDouble(bits);
    }

    private static String readText(byte[] data, int byteOff) {
        if (byteOff + 2 > data.length) return "";
        // Read length (RS - 2 bytes)
        short len = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        if (len <= 0 || byteOff + 2 + len > data.length) return "";
        return new String(data, byteOff + 2, len);
    }
}
