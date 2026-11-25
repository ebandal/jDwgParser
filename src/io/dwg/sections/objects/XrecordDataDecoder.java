package io.dwg.sections.objects;

import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.XDataRecord;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Decodes the data bytes of an XRECORD (spec §20.4.104): pairs of an RS group code and a value
 * whose form follows from the DXF group code. The specification names strings (R13–R2004 length
 * and code page, R2007+ an RS length and UTF-16 characters), 70 (a short), 10 (three doubles) and
 * 40 (a double), "following the normal DXF convention"; the other groups take the forms of their
 * DXF value types. Checked with the samples: the items of all 6,185 XRECORDs end exactly at their
 * data length. Two points the specification leaves open were settled that way: the R13–R2004
 * string length is an RS (then an RC code page), and binary chunks (310–319, 1004) have an RC
 * length. Values are little-endian; handles are 8 bytes, most significant first, as in the EED
 * (none of the 1003 / 1005 groups occurs in the samples).
 */
public final class XrecordDataDecoder {

    /** Group code of a record that holds the bytes that could not be read. */
    public static final int UNREAD = -1;

    private enum Form { STRING, POINT, DOUBLE, SHORT, INT, LONG, BYTE, BOOL, BINARY, HANDLE }

    private final byte[] data;
    private final boolean unicode;
    private final Charset charset;
    private int pos;

    private XrecordDataDecoder(byte[] data, DwgVersion version, Charset charset) {
        this.data = data;
        this.unicode = version.from(DwgVersion.R2007);
        this.charset = charset != null ? charset : StandardCharsets.ISO_8859_1;
    }

    /** The items of {@code data}; when an item cannot be read, the rest is one {@link #UNREAD} record. */
    public static List<XDataRecord> decode(byte[] data, DwgVersion version, Charset charset) {
        return new XrecordDataDecoder(data, version, charset).items();
    }

    private List<XDataRecord> items() {
        List<XDataRecord> out = new ArrayList<>();
        while (pos < data.length) {
            int start = pos;
            Object value = null;
            int code = -1;
            if (has(2)) {
                code = (int) little(2);
                Form form = form(code);
                value = form == null ? null : read(form);
            }
            if (value == null) {
                out.add(new XDataRecord(UNREAD, Arrays.copyOfRange(data, start, data.length)));
                break;
            }
            out.add(new XDataRecord(code, value));
        }
        return out;
    }

    /** The value form of a DXF group code, or {@code null} for codes that have none. */
    private static Form form(int code) {
        if (code >= 0 && code <= 9) return Form.STRING;
        if (code >= 10 && code <= 39) return Form.POINT;
        if (code >= 40 && code <= 59) return Form.DOUBLE;
        if (code >= 60 && code <= 79) return Form.SHORT;
        if (code >= 90 && code <= 99) return Form.INT;
        if (code == 100 || code == 101 || code == 102) return Form.STRING;
        if (code == 105) return Form.HANDLE;
        if (code >= 110 && code <= 119) return Form.POINT;
        if (code >= 120 && code <= 149) return Form.DOUBLE;
        if (code >= 160 && code <= 169) return Form.LONG;
        if (code >= 170 && code <= 179) return Form.SHORT;
        if (code >= 210 && code <= 219) return Form.POINT;
        if (code >= 220 && code <= 239) return Form.DOUBLE;
        if (code >= 270 && code <= 279) return Form.SHORT;
        if (code >= 280 && code <= 289) return Form.BYTE;
        if (code >= 290 && code <= 299) return Form.BOOL;
        if (code >= 300 && code <= 309) return Form.STRING;
        if (code >= 310 && code <= 319) return Form.BINARY;
        if (code >= 320 && code <= 369) return Form.HANDLE;
        if (code >= 370 && code <= 389) return Form.SHORT;
        if (code >= 390 && code <= 399) return Form.HANDLE;
        if (code >= 400 && code <= 409) return Form.SHORT;
        if (code >= 410 && code <= 419) return Form.STRING;
        if (code >= 420 && code <= 429) return Form.INT;
        if (code >= 430 && code <= 439) return Form.STRING;
        if (code >= 440 && code <= 449) return Form.INT;
        if (code >= 450 && code <= 459) return Form.INT;
        if (code >= 460 && code <= 469) return Form.DOUBLE;
        if (code >= 470 && code <= 479) return Form.STRING;
        if (code >= 480 && code <= 481) return Form.HANDLE;
        if (code == 999) return Form.STRING;
        if (code == 1004) return Form.BINARY;
        if (code == 1003 || code == 1005) return Form.HANDLE;
        if (code >= 1000 && code <= 1009) return Form.STRING;
        if (code >= 1010 && code <= 1059) return code <= 1039 ? Form.POINT : Form.DOUBLE;
        if (code >= 1060 && code <= 1070) return Form.SHORT;
        if (code == 1071) return Form.INT;
        return null;
    }

    private Object read(Form form) {
        switch (form) {
            case STRING: return readString();
            case POINT:  return has(24) ? new Point3D(dbl(), dbl(), dbl()) : null;
            case DOUBLE: return has(8) ? dbl() : null;
            case SHORT:  return has(2) ? (short) little(2) : null;
            case INT:    return has(4) ? (int) little(4) : null;
            case LONG:   return has(8) ? little(8) : null;
            case BYTE:   return has(1) ? data[pos++] & 0xFF : null;
            case BOOL:   return has(1) ? data[pos++] != 0 : null;
            case BINARY: {
                if (!has(1)) return null;
                int n = data[pos++] & 0xFF;
                return has(n) ? take(n) : null;
            }
            case HANDLE: {
                if (!has(8)) return null;
                long h = 0;
                for (int i = 0; i < 8; i++) h = (h << 8) | (data[pos++] & 0xFF);
                return new DwgHandleRef(h);
            }
            default: return null;
        }
    }

    private String readString() {
        if (unicode) {
            if (!has(2)) return null;
            int n = (int) little(2);
            return has(2 * n) ? new String(take(2 * n), StandardCharsets.UTF_16LE) : null;
        }
        if (!has(3)) return null;
        int n = (int) little(2);
        pos++;                                     // code page
        return has(n) ? new String(take(n), charset) : null;
    }

    private boolean has(int n) {
        return n >= 0 && pos + n <= data.length;
    }

    private byte[] take(int n) {
        byte[] b = Arrays.copyOfRange(data, pos, pos + n);
        pos += n;
        return b;
    }

    private double dbl() {
        return Double.longBitsToDouble(little(8));
    }

    private long little(int n) {
        long v = 0;
        for (int i = 0; i < n; i++) v |= (long) (data[pos++] & 0xFF) << (8 * i);
        return v;
    }
}
