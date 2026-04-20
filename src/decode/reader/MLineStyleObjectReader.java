package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgMLineStyle;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;

public class MLineStyleObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.MLINESTYLE.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgMLineStyle)) return;
        DwgMLineStyle mlinestyle = (DwgMLineStyle) target;

        int byteOff = offset;

        // Name (T)
        if (byteOff + 2 <= data.length) {
            String name = readText(data, byteOff);
            mlinestyle.setName(name);
            byteOff += 2 + name.length();
        }

        // Description (T)
        if (byteOff + 2 <= data.length) {
            String description = readText(data, byteOff);
            mlinestyle.setDescription(description);
            byteOff += 2 + description.length();
        }

        // Flags (BS)
        if (byteOff + 2 <= data.length) {
            int flags = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            mlinestyle.setFlags(flags);
        }
    }

    private static String readText(byte[] data, int byteOff) {
        if (byteOff + 2 > data.length) return "";
        short len = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        if (len <= 0 || byteOff + 2 + len > data.length) return "";
        return new String(data, byteOff + 2, len);
    }
}
