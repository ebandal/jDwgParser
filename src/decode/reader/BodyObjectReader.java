package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgBody;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;

public class BodyObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.BODY.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgBody)) return;
        DwgBody body = (DwgBody) target;

        int byteOff = offset;

        // Modeler format version (BL)
        if (byteOff + 4 <= data.length) {
            int modelerVersion = ByteBuffer.wrap(data, byteOff, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            body.setNumModelerFormatVersion(modelerVersion);
            byteOff += 4;
        }

        // Modeler geometry data blob (LL + data)
        if (byteOff + 4 <= data.length) {
            int dataLen = ByteBuffer.wrap(data, byteOff, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            byteOff += 4;
            if (dataLen > 0 && byteOff + dataLen <= data.length) {
                byte[] geomData = new byte[dataLen];
                System.arraycopy(data, byteOff, geomData, 0, dataLen);
                body.setModelerGeometryData(geomData);
            }
        }
    }
}
