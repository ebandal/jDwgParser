package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.DwgPolylineMesh;

public class PolylineMeshObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.POLYLINE_MESH.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgPolylineMesh)) return;
        DwgPolylineMesh pmesh = (DwgPolylineMesh) target;

        int byteOff = offset;

        // Flags (BS)
        if (byteOff + 2 <= data.length) {
            int flags = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            pmesh.setFlags(flags);
            byteOff += 2;
        }

        // M vertex count (BS)
        if (byteOff + 2 <= data.length) {
            int mcount = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            pmesh.setMVertexCount(mcount);
            byteOff += 2;
        }

        // N vertex count (BS)
        if (byteOff + 2 <= data.length) {
            int ncount = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            pmesh.setNVertexCount(ncount);
            byteOff += 2;
        }

        // M density (BS)
        if (byteOff + 2 <= data.length) {
            int mdensity = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            pmesh.setMDensity(mdensity);
            byteOff += 2;
        }

        // N density (BS)
        if (byteOff + 2 <= data.length) {
            int ndensity = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            pmesh.setNDensity(ndensity);
            byteOff += 2;
        }

        // Extrusion (3 × BD, optional)
        if (byteOff + 24 <= data.length) {
            double[] extr = new double[3];
            extr[0] = readBitDouble(data, byteOff);
            extr[1] = readBitDouble(data, byteOff + 8);
            extr[2] = readBitDouble(data, byteOff + 16);
            pmesh.setExtrusion(extr);
        }
    }

    private static double readBitDouble(byte[] data, int byteOff) {
        if (byteOff + 8 > data.length) return 0.0;
        long bits = ByteBuffer.wrap(data, byteOff, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        return Double.longBitsToDouble(bits);
    }
}
