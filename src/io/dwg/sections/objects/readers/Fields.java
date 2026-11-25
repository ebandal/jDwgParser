package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.entities.ValueHolder;

/**
 * Reads fields listed as in the specification's tables, "NAME TYPE", into an object's named
 * values. Types: B, RC, BS, BL, RL, BD, RD, 2RD, 2BD, 3BD, CMC, TV, X2 and X4 (2 and 4 raw bits).
 */
final class Fields {

    private Fields() {
    }

    static void read(ValueHolder record, BitStreamReader r, String... specs) {
        for (String spec : specs) {
            int space = spec.indexOf(' ');
            record.setValue(spec.substring(0, space), read(spec.substring(space + 1), r));
        }
    }

    static Object read(String type, BitStreamReader r) {
        switch (type) {
            case "B":   return r.getInput().readBit();
            case "RC":  return r.readRawChar();
            case "BS":  return r.readBitShort();
            case "BL":  return r.readBitLong();
            case "BD":  return r.readBitDouble();
            case "RD":  return r.readRawDouble();
            case "RL":  return r.getInput().readRawLong();
            case "2BD": { double[] p = r.read2BitDouble(); return new Point3D(p[0], p[1], 0); }
            case "2RD": { double[] p = r.read2RawDouble(); return new Point3D(p[0], p[1], 0); }
            case "3BD": { double[] p = r.read3BitDouble(); return new Point3D(p[0], p[1], p[2]); }
            case "CMC": return Colors.of(r.readCmColor());
            case "TV":  return r.readVariableText();
            case "X2":  return r.getInput().readBits(2);
            case "X4":  return r.getInput().readBits(4);
            default:    throw new IllegalArgumentException("unknown field type " + type);
        }
    }
}
