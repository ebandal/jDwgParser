package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.CmColor;
import structure.DwgVersion;
import structure.entities.DwgLayer;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;

public class LayerObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.LAYER.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgLayer)) return;
        DwgLayer layer = (DwgLayer) target;

        int byteOff = offset;

        // Flags (BS - 2 bytes)
        if (byteOff + 2 <= data.length) {
            int flags = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            layer.setFlags(flags);
            layer.setFrozen((flags & 0x01) != 0);
            layer.setOn((flags & 0x02) == 0); // bit 1 means "off"
            layer.setFrozenInNewViewports((flags & 0x04) != 0);
            layer.setLocked((flags & 0x08) != 0);
            byteOff += 2;
        }

        // Name (T - text string)
        if (byteOff + 2 <= data.length) {
            String name = readText(data, byteOff);
            layer.setName(name);
            byteOff += 2 + name.length();
        }

        // Line weight (BS)
        if (byteOff + 2 <= data.length) {
            short lw = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            layer.setLineWeight((double) lw);
            byteOff += 2;
        }

        // Color (CMC - 4 bytes: index + RGB)
        if (byteOff + 4 <= data.length) {
            CmColor color = new CmColor();
            color.colorIndex = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            color.rgbValue = ByteBuffer.wrap(data, byteOff + 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            layer.setColor(color);
            byteOff += 4;
        }
    }

    private static String readText(byte[] data, int byteOff) {
        if (byteOff + 2 > data.length) return "";
        short len = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        if (len <= 0 || byteOff + 2 + len > data.length) return "";
        return new String(data, byteOff + 2, len);
    }
}
