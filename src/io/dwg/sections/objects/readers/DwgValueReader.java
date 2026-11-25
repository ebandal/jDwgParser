package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The value of spec §20.4.99, part of FIELD and of table cells. Read into a map:
 * <pre>
 *   R2007+ FLAGS BL (bit 1: the data is not stored)
 *   TYPE BL; unless bit 1 of FLAGS: DATA by TYPE: 0 unknown BL, 1 long BL, 2 double BD,
 *     4 string TV, 8 date (BL size, bytes), 16 point (BL size, 2RD), 32 3D point (BL size, 3RD),
 *     64 object id (a handle, in the handle stream: {@link #OBJECT_ID} counts them),
 *     512 general (BL size, bytes)
 *   R2007+ UNITS BL, FORMAT TV, VALUESTRING TV
 * </pre>
 */
final class DwgValueReader {

    /** Data type of a value that refers to an object; its handle is in the handle stream. */
    static final int OBJECT_ID = 64;

    private DwgValueReader() {
    }

    static Map<String, Object> read(BitStreamReader r, DwgVersion v) {
        Map<String, Object> value = new LinkedHashMap<>();
        int flags = 0;
        if (v.from(DwgVersion.R2007)) {
            flags = r.readBitLong();
            value.put("FLAGS", flags);
        }
        int type = r.readBitLong();
        value.put("TYPE", type);
        if ((flags & 1) == 0) {
            switch (type) {
                case 0:
                case 1:  value.put("DATA", r.readBitLong()); break;
                case 2:  value.put("DATA", r.readBitDouble()); break;
                case 4:  value.put("DATA", r.readVariableText()); break;
                case 8:
                case 512: value.put("DATA", bytes(r, r.readBitLong())); break;
                case 16: {
                    r.readBitLong();
                    double[] p = r.read2RawDouble();
                    value.put("DATA", new Point3D(p[0], p[1], 0));
                    break;
                }
                case 32: {
                    r.readBitLong();
                    double[] p = r.read3RawDouble();
                    value.put("DATA", new Point3D(p[0], p[1], p[2]));
                    break;
                }
                case OBJECT_ID: break;
                default: throw new IllegalStateException("value data type " + type + " is not described");
            }
        }
        if (v.from(DwgVersion.R2007)) {
            value.put("UNITS", r.readBitLong());
            value.put("FORMAT", r.readVariableText());
            value.put("VALUESTRING", r.readVariableText());
        }
        return value;
    }

    private static byte[] bytes(BitStreamReader r, int n) {
        if (n < 0) {
            throw new IllegalStateException("negative value size " + n);
        }
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++) {
            b[i] = (byte) r.readRawChar();
        }
        return b;
    }
}
