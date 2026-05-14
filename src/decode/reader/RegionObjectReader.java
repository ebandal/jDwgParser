package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.DwgRegion;

public class RegionObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.REGION.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgRegion)) return;
        DwgRegion region = (DwgRegion) target;

        int byteOff = offset;

        // Modeler format version (BL)
        if (byteOff + 4 <= data.length) {
            int modelerVersion = ByteBuffer.wrap(data, byteOff, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            region.setNumModelerFormatVersion(modelerVersion);
            byteOff += 4;
        }

        // Modeler geometry data blob (LL + data)
        if (byteOff + 4 <= data.length) {
            int dataLen = ByteBuffer.wrap(data, byteOff, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            byteOff += 4;
            if (dataLen > 0 && byteOff + dataLen <= data.length) {
                byte[] geomData = new byte[dataLen];
                System.arraycopy(data, byteOff, geomData, 0, dataLen);
                region.setModelerGeometryData(geomData);
            }
        }
    }
}
