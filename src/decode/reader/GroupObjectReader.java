package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgGroup;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;

public class GroupObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.GROUP.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgGroup)) return;
        DwgGroup group = (DwgGroup) target;

        int byteOff = offset;

        // Group name (T)
        if (byteOff + 2 <= data.length) {
            String name = readText(data, byteOff);
            group.setGroupName(name);
            byteOff += 2 + name.length();
        }

        // Unnamed flag (B)
        if (byteOff + 1 <= data.length) {
            int unnamed = data[byteOff] & 0xFF;
            group.setUnnamed(unnamed != 0);
            byteOff += 1;
        }

        // Selectable flag (B)
        if (byteOff + 1 <= data.length) {
            int selectable = data[byteOff] & 0xFF;
            group.setSelectable(selectable != 0);
        }
    }

    private static String readText(byte[] data, int byteOff) {
        if (byteOff + 2 > data.length) return "";
        short len = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        if (len <= 0 || byteOff + 2 + len > data.length) return "";
        return new String(data, byteOff + 2, len);
    }
}
