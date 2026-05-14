package decode.reader;

import structure.DwgVersion;
import structure.entities.DwgLine;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.Point3D;

public class LineObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.LINE.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgLine)) return;
        DwgLine line = (DwgLine) target;

        int bitOffset = 0;

        // Z flag: if true, Z coordinates are zero (common optimization)
        boolean zAreZero = readBit(data, offset, bitOffset);
        bitOffset += 1;

        // Start/End X coordinates (RD - raw doubles)
        double sx = readRawDouble(data, offset, bitOffset);
        bitOffset += 64;
        double ex = readRawDouble(data, offset, bitOffset);
        bitOffset += 64;

        // Start/End Y coordinates
        double sy = readRawDouble(data, offset, bitOffset);
        bitOffset += 64;
        double ey = readRawDouble(data, offset, bitOffset);
        bitOffset += 64;

        // Start/End Z coordinates (conditional)
        double sz = zAreZero ? 0.0 : readRawDouble(data, offset, bitOffset);
        if (!zAreZero) bitOffset += 64;

        double ez = zAreZero ? 0.0 : readRawDouble(data, offset, bitOffset);
        if (!zAreZero) bitOffset += 64;

        line.setStart(new Point3D(sx, sy, sz));
        line.setEnd(new Point3D(ex, ey, ez));

        // Thickness (BD - bit double, optional)
        if (hasThickness(version)) {
            line.setThickness(readBitDouble(data, offset, bitOffset));
            bitOffset += 16; // approximate
        }

        // Extrusion (BE - bit extrusion, optional)
        if (hasExtrusion(version)) {
            double[] extr = read3BitDouble(data, offset, bitOffset);
            line.setExtrusion(extr);
        }
    }

    private static boolean readBit(byte[] data, int byteOff, int bitOff) {
        if (byteOff >= data.length) return false;
        return ((data[byteOff] >> (7 - bitOff)) & 1) == 1;
    }

    private static double readRawDouble(byte[] data, int byteOff, int bitOff) {
        // Raw double: 8 bytes, little-endian (assuming byte-aligned)
        int alignedOff = byteOff + (bitOff / 8);
        if (alignedOff + 8 > data.length) return 0.0;

        long bits = 0;
        for (int i = 0; i < 8; i++) {
            bits |= ((long) data[alignedOff + i] & 0xFF) << (i * 8);
        }
        return Double.longBitsToDouble(bits);
    }

    private static double readBitDouble(byte[] data, int byteOff, int bitOff) {
        // Simplified: read as raw double
        return readRawDouble(data, byteOff, bitOff);
    }

    private static double[] read3BitDouble(byte[] data, int byteOff, int bitOff) {
        return new double[] {
            readBitDouble(data, byteOff, bitOff),
            readBitDouble(data, byteOff + 8, bitOff),
            readBitDouble(data, byteOff + 16, bitOff)
        };
    }

    private static boolean hasThickness(DwgVersion v) {
        return true; // R2004+ has thickness
    }

    private static boolean hasExtrusion(DwgVersion v) {
        return true; // R2004+ has extrusion
    }
}
