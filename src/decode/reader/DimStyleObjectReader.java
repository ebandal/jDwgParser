package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgDimStyle;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;

public class DimStyleObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.DIMSTYLE.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgDimStyle)) return;
        DwgDimStyle dimstyle = (DwgDimStyle) target;

        int byteOff = offset;

        // Name (T)
        if (byteOff + 2 <= data.length) {
            String name = readText(data, byteOff);
            dimstyle.setName(name);
            byteOff += 2 + name.length();
        }

        // Text height (BD)
        if (byteOff + 8 <= data.length) {
            double textHeight = readBitDouble(data, byteOff);
            dimstyle.setTextHeight(textHeight);
            byteOff += 8;
        }

        // Text gap (BD)
        if (byteOff + 8 <= data.length) {
            double textGap = readBitDouble(data, byteOff);
            dimstyle.setTextGap(textGap);
            byteOff += 8;
        }

        // Arrow size (BD)
        if (byteOff + 8 <= data.length) {
            double arrowSize = readBitDouble(data, byteOff);
            dimstyle.setArrowSize(arrowSize);
            byteOff += 8;
        }

        // Line extension (BD)
        if (byteOff + 8 <= data.length) {
            double lineExt = readBitDouble(data, byteOff);
            dimstyle.setLineExtension(lineExt);
            byteOff += 8;
        }

        // Line offset (BD)
        if (byteOff + 8 <= data.length) {
            double lineOffset = readBitDouble(data, byteOff);
            dimstyle.setLineOffset(lineOffset);
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
