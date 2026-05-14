package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgBlockHeader;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.Point3D;

public class BlockHeaderObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.BLOCK_HEADER.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgBlockHeader)) return;
        DwgBlockHeader block = (DwgBlockHeader) target;

        int byteOff = offset;

        // Flags (BS)
        if (byteOff + 2 <= data.length) {
            int flags = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            block.setFlags(flags);
            byteOff += 2;
        }

        // Block name (T - text string)
        if (byteOff + 2 <= data.length) {
            String name = readText(data, byteOff);
            block.setBlockName(name);
            byteOff += 2 + name.length();
        }

        // Block insertion point (3 × BD)
        if (byteOff + 24 <= data.length) {
            double x = readBitDouble(data, byteOff);
            double y = readBitDouble(data, byteOff + 8);
            double z = readBitDouble(data, byteOff + 16);
            block.setBasePoint(new Point3D(x, y, z));
            byteOff += 24;
        }

        // XREF path name (T, optional)
        if (byteOff + 2 <= data.length) {
            String xref = readText(data, byteOff);
            if (!xref.isEmpty()) {
                block.setXrefPath(xref);
            }
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
