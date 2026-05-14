package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgLtype;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;

public class LtypeObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.LTYPE.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgLtype)) return;
        DwgLtype ltype = (DwgLtype) target;

        int byteOff = offset;

        // Flags (BS)
        if (byteOff + 2 <= data.length) {
            byteOff += 2;
        }

        // Line type name (T)
        if (byteOff + 2 <= data.length) {
            String name = readText(data, byteOff);
            ltype.setName(name);
            byteOff += 2 + name.length();
        }

        // Description (T)
        if (byteOff + 2 <= data.length) {
            String desc = readText(data, byteOff);
            ltype.setDescription(desc);
            byteOff += 2 + desc.length();
        }

        // Pattern length (BD)
        if (byteOff + 8 <= data.length) {
            double patLen = readBitDouble(data, byteOff);
            ltype.setTotalLength(patLen);
            byteOff += 8;
        }

        // Number of dash items (BS)
        if (byteOff + 2 <= data.length) {
            short numDashes = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            ltype.setNumDashes((int) (numDashes & 0xFFFF));
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
