package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.DwgStyle;

public class StyleObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.STYLE.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgStyle)) return;
        DwgStyle style = (DwgStyle) target;

        int byteOff = offset;

        // Name (T)
        if (byteOff + 2 <= data.length) {
            String name = readText(data, byteOff);
            style.setName(name);
            byteOff += 2 + name.length();
        }

        // Flags (BS)
        if (byteOff + 2 <= data.length) {
            int flags = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            style.setFlags(flags);
            byteOff += 2;
        }

        // Width (BD)
        if (byteOff + 8 <= data.length) {
            double width = readBitDouble(data, byteOff);
            style.setWidth(width);
            byteOff += 8;
        }

        // Oblique angle (BD)
        if (byteOff + 8 <= data.length) {
            double oblique = readBitDouble(data, byteOff);
            style.setOblique(oblique);
            byteOff += 8;
        }

        // Font filename (T)
        if (byteOff + 2 <= data.length) {
            String fontFile = readText(data, byteOff);
            style.setFontFilename(fontFile);
            byteOff += 2 + fontFile.length();
        }

        // Big font filename (T, optional)
        if (byteOff + 2 <= data.length) {
            String bigFont = readText(data, byteOff);
            style.setBigFontFilename(bigFont);
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
