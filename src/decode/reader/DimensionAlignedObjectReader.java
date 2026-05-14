package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgDimensionAligned;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.Point3D;

public class DimensionAlignedObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.DIMENSION_ALIGNED.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgDimensionAligned)) return;
        DwgDimensionAligned dim = (DwgDimensionAligned) target;

        int byteOff = offset;

        // Definition point (3 × BD)
        double x = readBitDouble(data, byteOff);
        byteOff += 8;
        double y = readBitDouble(data, byteOff);
        byteOff += 8;
        double z = readBitDouble(data, byteOff);
        byteOff += 8;
        dim.setDefinitionPoint(new Point3D(x, y, z));

        // Midpoint of text (3 × BD)
        if (byteOff + 24 <= data.length) {
            double mx = readBitDouble(data, byteOff);
            double my = readBitDouble(data, byteOff + 8);
            double mz = readBitDouble(data, byteOff + 16);
            dim.setMidpointOfText(new Point3D(mx, my, mz));
            byteOff += 24;
        }

        // Text (T)
        if (byteOff + 2 <= data.length) {
            String text = readText(data, byteOff);
            dim.setText(text);
            byteOff += 2 + text.length();
        }

        // Text rotation (BD)
        if (byteOff + 8 <= data.length) {
            double textRot = readBitDouble(data, byteOff);
            dim.setTextRotation(textRot);
            byteOff += 8;
        }

        // Insertion scale (BD)
        if (byteOff + 8 <= data.length) {
            double scale = readBitDouble(data, byteOff);
            dim.setInsertionScale(scale);
            byteOff += 8;
        }

        // Dimension style name (T, optional)
        if (byteOff + 2 <= data.length) {
            String style = readText(data, byteOff);
            dim.setDimensionStyleName(style);
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
