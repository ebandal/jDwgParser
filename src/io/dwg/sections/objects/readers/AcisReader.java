package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.AbstractDwgAcisEntity;
import io.dwg.entities.DwgObject;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * REGION, 3DSOLID and BODY, spec §20.4.41, after the common entity data:
 * <pre>
 *   ACIS empty B; unknown B; version BS
 *     version 1: blocks of BL size and that many RC, until a size of 0; the characters
 *                0x20–0x7E are stored as 0x9F minus the character (so a space is 0x7F), the
 *                others as they are
 *     version 2: a SAT or SAB file ("ACIS BinaryFile"), up to its end marker
 *   wireframe present B; if so: point present B, point 3BD, number of isolines BL,
 *     isolines present B, number of wires BL, wires; number of silhouettes BL, silhouettes
 *   ACIS empty B (normally 1; if 0, more ACIS data in the same form, without wireframe)
 *   R2007+ unknown BL
 *   R2013+ unknown B, GUID (BL, BS, BS, 8 RC), unknown BL
 *   handles: R2007+ history
 * </pre>
 * A wire: type RC, selection marker BL, color BS, ACIS index BL, number of points BL, points 3BD,
 * transform present B and, if so, X, Y, Z axes and translation 3BD, scale BD, has rotation B,
 * has reflection B, has shear B. A silhouette: viewport id BL, target, direction from target and
 * up direction 3BD, perspective B, an unknown B, number of wires BL and the wires.
 * <p>The unknown bit before the silhouette's wire count is not in the specification. Without it
 * all 64 R2000 solids of the samples that have a silhouette fail; with it, all 207 R2000 ACIS
 * entities end their data exactly.
 * <p>R2013+: the model is in the data storage section (AcDb:AcDsPrototype_1b, chapter 24) under
 * the entity's handle, and the ACIS empty bit is set. The specification does not describe the
 * entity for these versions; found on the 6 R2013 and R2018 ACIS entities of the samples (copies
 * of the same 3, whose R2010 copies give the same wireframe):
 * <ul>
 *   <li>After the empty bit there is one bit (1 in all of them) and then the wireframe point;
 *       of the wireframe present and point present bits only one is there. It is read as the
 *       wireframe present bit, and the point as always present.</li>
 *   <li>After the R2007+ unknown BL: a bit, a GUID and a BL that is 0 in all of them. One
 *       entity has the bit 0 and a null GUID; the other two have the bit 1 and GUIDs of the
 *       version 4 and version 3 forms, the same in the R2013 and R2018 copies.</li>
 * </ul>
 * With these all 6 end their data exactly. Their model is attached by {@code DwgReader}.
 * <p>R13: one of the two counts after the isolines, the wires' or the silhouettes', is not there.
 * In the 3 R13 ACIS entities of the samples (whose R14 copies have no wires and no silhouettes)
 * the data ends 2 bits before the end that the specification's fields give, and without one
 * BL of value 0 there all end exactly, with the ACIS empty bit at the end set as usual. It is
 * read as the silhouette count being absent; the samples do not tell which.
 */
abstract class AcisReader implements ObjectReader {

    private static final byte[] SAB_HEADER = "ACIS BinaryFile".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] SAB_END =
            "End\u000E\u0002of\u000E\u0004ACIS\r\u0004data".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] SAT_END = "End-of-ACIS-data".getBytes(StandardCharsets.ISO_8859_1);

    @Override
    public final void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);
        AbstractDwgAcisEntity e = (AbstractDwgAcisEntity) target;

        boolean empty = r.getInput().readBit();
        e.setEmpty(empty);
        if (!empty) {
            r.getInput().readBit();                // unknown
            readModel(e, r);
        }
        boolean stored = empty && v.from(DwgVersion.R2013);
        if (r.getInput().readBit()) {              // wireframe present
            boolean pointPresent = stored || r.getInput().readBit();
            e.setWireframePoint(pointPresent ? point(r) : Point3D.of(0, 0, 0));
            e.setIsolineCount(r.readBitLong());
            r.getInput().readBit();                // isolines present
            int wires = r.readBitLong();
            for (int i = 0; i < wires; i++) {
                e.addWire(wire(r));
            }
            int silhouettes = v.from(DwgVersion.R14) ? r.readBitLong() : 0;
            for (int i = 0; i < silhouettes; i++) {
                int id = r.readBitLong();
                Point3D t = point(r);
                Point3D d = point(r);
                Point3D up = point(r);
                boolean perspective = r.getInput().readBit();
                r.getInput().readBit();            // unknown, not in the specification
                int n = r.readBitLong();
                List<AbstractDwgAcisEntity.Wire> list = new ArrayList<>();
                for (int j = 0; j < n; j++) {
                    list.add(wire(r));
                }
                e.addSilhouette(new AbstractDwgAcisEntity.Silhouette(id, t, d, up, perspective, list));
            }
        }
        if (!r.getInput().readBit()) {             // ACIS empty: 0 means more data
            r.getInput().readBit();
            readModel(e, r);
        }
        if (v.from(DwgVersion.R2007)) {
            r.readBitLong();                       // unknown
        }
        if (v.from(DwgVersion.R2013)) {
            r.getInput().readBit();                // unknown
            long data1 = r.readBitLong() & 0xFFFFFFFFL;
            long data2 = r.readBitShort() & 0xFFFF;
            long data3 = r.readBitShort() & 0xFFFF;
            long data4 = 0;
            for (int i = 0; i < 8; i++) {
                data4 = data4 << 8 | r.readRawChar();
            }
            e.setGuid(new java.util.UUID(data1 << 32 | data2 << 16 | data3, data4));
            r.readBitLong();                       // unknown
        }
    }

    private static void readModel(AbstractDwgAcisEntity e, BitStreamReader r) {
        int version = r.readBitShort();
        e.setAcisVersion(version);
        if (version == 1) {
            StringBuilder text = new StringBuilder();
            if (e.satText() != null) text.append(e.satText());
            for (int size = r.readBitLong(); size > 0; size = r.readBitLong()) {
                for (int i = 0; i < size; i++) {
                    int c = r.readRawChar();
                    int decoded = 0x9F - c;
                    if (decoded >= 0x20 && decoded <= 0x7E) {
                        c = decoded;
                    }
                    text.append(c == '\t' ? ' ' : (char) c);
                }
            }
            e.setSatText(text.toString());
        } else if (version == 2) {
            byte[] b = new byte[4096];
            int n = 0;
            byte[] end = null;
            while (true) {
                if (n == b.length) {
                    b = java.util.Arrays.copyOf(b, b.length * 2);
                }
                b[n++] = (byte) r.readRawChar();
                if (end == null && n == SAB_HEADER.length) {
                    end = startsWith(b, SAB_HEADER) ? SAB_END : SAT_END;
                }
                if (end != null && endsWith(b, n, end)) {
                    break;
                }
            }
            b = java.util.Arrays.copyOf(b, n);
            if (end == SAB_END) {
                e.setSabData(b);
            } else {
                e.setSatText(new String(b, StandardCharsets.ISO_8859_1));
            }
        } else {
            throw new IllegalStateException("unknown ACIS data version " + version);
        }
    }

    private static AbstractDwgAcisEntity.Wire wire(BitStreamReader r) {
        int type = r.readRawChar();
        int marker = r.readBitLong();
        int color = r.readBitShort();
        int index = r.readBitLong();
        int n = r.readBitLong();
        List<Point3D> points = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            points.add(point(r));
        }
        double[] transform = null;
        if (r.getInput().readBit()) {
            transform = new double[13];
            for (int i = 0; i < 12; i += 3) {
                double[] p = r.read3BitDouble();
                transform[i] = p[0];
                transform[i + 1] = p[1];
                transform[i + 2] = p[2];
            }
            transform[12] = r.readBitDouble();
            r.getInput().readBit();                // has rotation
            r.getInput().readBit();                // has reflection
            r.getInput().readBit();                // has shear
        }
        return new AbstractDwgAcisEntity.Wire(type, marker, color, index, points, transform);
    }

    private static Point3D point(BitStreamReader r) {
        double[] p = r.read3BitDouble();
        return Point3D.of(p[0], p[1], p[2]);
    }

    private static boolean startsWith(byte[] b, byte[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            if (b[i] != prefix[i]) return false;
        }
        return true;
    }

    private static boolean endsWith(byte[] b, int n, byte[] suffix) {
        if (n < suffix.length) return false;
        for (int i = 0; i < suffix.length; i++) {
            if (b[n - suffix.length + i] != suffix[i]) return false;
        }
        return true;
    }

    /** Whether the R2007+ handles hold the history handle. */
    protected boolean hasHistoryHandle() {
        return true;
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        if (v.from(DwgVersion.R2007) && hasHistoryHandle()) {
            ((AbstractDwgAcisEntity) target).setHistoryHandle(r.readRef(target.handle()));
        }
    }
}
