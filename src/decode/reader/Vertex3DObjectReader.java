package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.DwgVertex3D;
import structure.entities.Point3D;

public class Vertex3DObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.VERTEX_3D.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgVertex3D)) return;
        DwgVertex3D vertex = (DwgVertex3D) target;

        int byteOff = offset;

        // Location (3 × BD)
        double x = readBitDouble(data, byteOff);
        byteOff += 8;
        double y = readBitDouble(data, byteOff);
        byteOff += 8;
        double z = readBitDouble(data, byteOff);
        byteOff += 8;
        vertex.setLocation(new Point3D(x, y, z));

        // Flags (BS)
        if (byteOff + 2 <= data.length) {
            int flags = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            vertex.setFlags(flags);
        }
    }

    private static double readBitDouble(byte[] data, int byteOff) {
        if (byteOff + 8 > data.length) return 0.0;
        long bits = ByteBuffer.wrap(data, byteOff, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        return Double.longBitsToDouble(bits);
    }
}
