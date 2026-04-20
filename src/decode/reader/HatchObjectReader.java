package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgHatch;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;

public class HatchObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.HATCH.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgHatch)) return;
        DwgHatch hatch = (DwgHatch) target;

        int byteOff = offset;

        // Pattern name (T)
        if (byteOff + 2 <= data.length) {
            String pattern = readText(data, byteOff);
            hatch.setPatternName(pattern);
            byteOff += 2 + pattern.length();
        }

        // Hatch style (BS)
        if (byteOff + 2 <= data.length) {
            int style = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            hatch.setHatchStyle(style);
            byteOff += 2;
        }

        // Scale (BD)
        if (byteOff + 8 <= data.length) {
            double scale = readBitDouble(data, byteOff);
            hatch.setScale(scale);
            byteOff += 8;
        }

        // Angle (BD)
        if (byteOff + 8 <= data.length) {
            double angle = readBitDouble(data, byteOff);
            hatch.setAngle(angle);
            byteOff += 8;
        }

        // Number of boundary paths (BS)
        if (byteOff + 2 <= data.length) {
            int numPaths = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            hatch.setNumBoundaryPaths(numPaths);
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
