package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgAttrib;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.Point3D;

public class AttribObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.ATTRIB.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgAttrib)) return;
        DwgAttrib attrib = (DwgAttrib) target;

        int byteOff = offset;

        // Insertion point (3 × BD)
        double x = readBitDouble(data, byteOff);
        byteOff += 8;
        double y = readBitDouble(data, byteOff);
        byteOff += 8;
        double z = readBitDouble(data, byteOff);
        byteOff += 8;
        attrib.setLocation(new Point3D(x, y, z));

        // Text height (BD)
        if (byteOff + 8 <= data.length) {
            double height = readBitDouble(data, byteOff);
            attrib.setHeight(height);
            byteOff += 8;
        }

        // Attribute value (T)
        if (byteOff + 2 <= data.length) {
            String text = readText(data, byteOff);
            attrib.setText(text);
            byteOff += 2 + text.length();
        }

        // Attribute tag (T)
        if (byteOff + 2 <= data.length) {
            String tag = readText(data, byteOff);
            attrib.setTag(tag);
            byteOff += 2 + tag.length();
        }

        // Text rotation (BD)
        if (byteOff + 8 <= data.length) {
            double rotation = readBitDouble(data, byteOff);
            attrib.setAngle(rotation);
            byteOff += 8;
        }

        // Style name (T, optional)
        if (byteOff + 2 <= data.length) {
            String style = readText(data, byteOff);
            attrib.setStyleName(style);
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
