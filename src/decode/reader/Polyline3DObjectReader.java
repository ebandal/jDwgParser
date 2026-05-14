package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.DwgPolyline3D;

public class Polyline3DObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.POLYLINE_3D.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgPolyline3D)) return;
        DwgPolyline3D pline = (DwgPolyline3D) target;

        int byteOff = offset;

        // Flags (BS)
        if (byteOff + 2 <= data.length) {
            int flags = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            pline.setFlags(flags);
        }
    }
}
