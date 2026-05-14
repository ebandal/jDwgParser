package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgAttdef;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.Point3D;

public class AttdefObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.ATTDEF.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgAttdef)) return;
        DwgAttdef attdef = (DwgAttdef) target;

        int byteOff = offset;

        // Insertion point (3 × BD)
        double x = readBitDouble(data, byteOff);
        byteOff += 8;
        double y = readBitDouble(data, byteOff);
        byteOff += 8;
        double z = readBitDouble(data, byteOff);
        byteOff += 8;
        attdef.setInsertionPoint(new Point3D(x, y, z));

        // Text height (BD)
        if (byteOff + 8 <= data.length) {
            double height = readBitDouble(data, byteOff);
            attdef.setTextHeight(height);
            byteOff += 8;
        }

        // Attribute tag (T)
        if (byteOff + 2 <= data.length) {
            String tag = readText(data, byteOff);
            attdef.setTag(tag);
            byteOff += 2 + tag.length();
        }

        // Default value (T)
        if (byteOff + 2 <= data.length) {
            String defaultValue = readText(data, byteOff);
            attdef.setDefaultValue(defaultValue);
            byteOff += 2 + defaultValue.length();
        }

        // Prompt (T)
        if (byteOff + 2 <= data.length) {
            String prompt = readText(data, byteOff);
            attdef.setPrompt(prompt);
            byteOff += 2 + prompt.length();
        }

        // Justification (BS)
        if (byteOff + 2 <= data.length) {
            int just = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            attdef.setJustification(just);
            byteOff += 2;
        }

        // Text rotation (BD)
        if (byteOff + 8 <= data.length) {
            double rotation = readBitDouble(data, byteOff);
            attdef.setRotation(rotation);
            byteOff += 8;
        }

        // Width factor (BD)
        if (byteOff + 8 <= data.length) {
            double widthFactor = readBitDouble(data, byteOff);
            attdef.setWidthFactor(widthFactor);
            byteOff += 8;
        }

        // Obliquing angle (BD)
        if (byteOff + 8 <= data.length) {
            double oblique = readBitDouble(data, byteOff);
            attdef.setObliquingAngle(oblique);
            byteOff += 8;
        }

        // Text string (T)
        if (byteOff + 2 <= data.length) {
            String text = readText(data, byteOff);
            attdef.setTextString(text);
            byteOff += 2 + text.length();
        }

        // Flags (BS)
        if (byteOff + 2 <= data.length) {
            int flags = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            attdef.setFlags(flags);
            byteOff += 2;
        }

        // Style name (T, optional)
        if (byteOff + 2 <= data.length) {
            String style = readText(data, byteOff);
            attdef.setStyleName(style);
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
