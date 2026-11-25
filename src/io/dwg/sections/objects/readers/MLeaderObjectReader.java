package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgMLeader;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MULTILEADER (a class), spec §20.4.48, with its embedded context MLeaderAnnotContext (§20.4.86),
 * read in the order and with the types of the specification's tables into the entity's values
 * (names after the fields' meaning). The handles the fields refer to are read from the handle
 * stream in the same order.
 * <p>Settled on the 15 MULTILEADERs of the samples (R14 to R2018 copies of the same drawings),
 * where the specification is unclear or differs; with these, all end their data and handles
 * exactly, and the copies agree:
 * <ul>
 *   <li>The embedded context starts with the scale handle; the fields of AcDbObjectContextData
 *       (§20.4.89) are not repeated.</li>
 *   <li>A leader line's break info: a count, then per entry the segment index and the start/end
 *       pairs (the table lists them once).</li>
 *   <li>The style block handle is present only when the content type is block (the table marks
 *       its DXF group as optional).</li>
 *   <li>The arrow head and block label lists ("-R2007" in the table) are present up to R2007;
 *       from R2010 only one list of the two is there. The samples' lists are empty, which does
 *       not tell which one; this reader takes the arrow heads.</li>
 * </ul>
 */
public class MLeaderObjectReader implements ObjectReader {

    private static final int BLOCK_CONTENT = 1;

    @Override
    public int objectType() { return DwgObjectType.MULTILEADER.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);
        DwgMLeader m = (DwgMLeader) target;
        boolean r2010 = v.from(DwgVersion.R2010);
        if (r2010) {
            Fields.read(m, r, "VERSION BS");
        }
        Map<String, Object> context = new LinkedHashMap<>();
        m.setValue("CONTEXT", context);
        readContext(context, r, r2010);

        CellStyles.slot(r, values(m), "STYLE");
        Fields.read(m, r, "OVERRIDES BL", "LEADERTYPE BS", "LEADERCOLOR CMC");
        CellStyles.slot(r, values(m), "LEADERLINETYPE");
        Fields.read(m, r, "LINEWEIGHT BL", "LANDING B", "DOGLEG B", "LANDINGDISTANCE BD");
        CellStyles.slot(r, values(m), "ARROWHEAD");
        Fields.read(m, r, "ARROWSIZE BD", "CONTENTTYPE BS");
        CellStyles.slot(r, values(m), "TEXTSTYLE");
        Fields.read(m, r, "TEXTLEFTATTACHMENT BS", "TEXTRIGHTATTACHMENT BS", "TEXTANGLETYPE BS",
                "UNKNOWN BS", "TEXTCOLOR CMC", "TEXTFRAME B");
        if (Integer.valueOf(BLOCK_CONTENT).equals(m.value("CONTENTTYPE"))) {
            CellStyles.slot(r, values(m), "BLOCK");
        }
        Fields.read(m, r, "BLOCKCOLOR CMC", "BLOCKSCALE 3BD", "BLOCKROTATION BD", "BLOCKATTACHMENT BS",
                "ANNOTATIVE B");
        {
            List<Map<String, Object>> arrows = new ArrayList<>();
            for (int i = r.readBitLong(); i > 0; i--) {
                Map<String, Object> a = new LinkedHashMap<>();
                a.put("DEFAULT", r.getInput().readBit());
                CellStyles.slot(r, a, "ARROWHEAD");
                arrows.add(a);
            }
            m.setValue("ARROWHEADS", arrows);
            List<Map<String, Object>> labels = new ArrayList<>();
            for (int i = r2010 ? 0 : r.readBitLong(); i > 0; i--) {
                Map<String, Object> l = new LinkedHashMap<>();
                CellStyles.slot(r, l, "ATTDEF");
                l.put("TEXT", r.readVariableText());
                l.put("INDEX", r.readBitShort());
                l.put("WIDTH", r.readBitDouble());
                labels.add(l);
            }
            m.setValue("BLOCKLABELS", labels);
        }
        Fields.read(m, r, "TEXTDIRECTIONNEGATIVE B", "IPEALIGN BS", "JUSTIFICATION BS", "SCALE BD");
        if (r2010) {
            Fields.read(m, r, "ATTACHMENTDIRECTION BS", "TEXTTOPATTACHMENT BS", "TEXTBOTTOMATTACHMENT BS");
        }
        if (v.from(DwgVersion.R2013)) {
            Fields.read(m, r, "EXTENDEDTOTEXT B");
        }
    }

    /** MLeaderAnnotContext, §20.4.86 (after the scale handle of §20.4.71). */
    private static void readContext(Map<String, Object> c, BitStreamReader r, boolean r2010) {
        CellStyles.slot(r, c, "SCALE");
        List<Map<String, Object>> roots = new ArrayList<>();
        c.put("ROOTS", roots);
        for (int i = r.readBitLong(); i > 0; i--) {
            Map<String, Object> root = new LinkedHashMap<>();
            roots.add(root);
            put(root, r, "CONTENTVALID B", "UNKNOWN B", "CONNECTION 3BD", "DIRECTION 3BD");
            root.put("BREAKS", pairs(r));
            put(root, r, "INDEX BL", "LANDINGDISTANCE BD");
            List<Map<String, Object>> lines = new ArrayList<>();
            root.put("LINES", lines);
            for (int j = r.readBitLong(); j > 0; j--) {
                Map<String, Object> line = new LinkedHashMap<>();
                lines.add(line);
                List<Object> points = new ArrayList<>();
                for (int k = r.readBitLong(); k > 0; k--) {
                    points.add(Fields.read("3BD", r));
                }
                line.put("POINTS", points);
                List<Map<String, Object>> breaks = new ArrayList<>();
                for (int k = r.readBitLong(); k > 0; k--) {
                    Map<String, Object> b = new LinkedHashMap<>();
                    put(b, r, "SEGMENTINDEX BL");
                    b.put("PAIRS", pairs(r));
                    breaks.add(b);
                }
                line.put("BREAKS", breaks);
                put(line, r, "INDEX BL");
                if (r2010) {
                    put(line, r, "LEADERTYPE BS", "COLOR CMC");
                    CellStyles.slot(r, line, "LINETYPE");
                    put(line, r, "LINEWEIGHT BL", "ARROWSIZE BD");
                    CellStyles.slot(r, line, "ARROWHEAD");
                    put(line, r, "OVERRIDES BL");
                }
            }
            if (r2010) {
                put(root, r, "ATTACHMENTDIRECTION BS");
            }
        }
        put(c, r, "SCALE BD", "BASE 3BD", "TEXTHEIGHT BD", "ARROWSIZE BD", "LANDINGGAP BD",
                "LEFTATTACHMENT BS", "RIGHTATTACHMENT BS", "TEXTALIGN BS", "ATTACHMENTTYPE BS", "HASTEXT B");
        if (Boolean.TRUE.equals(c.get("HASTEXT"))) {
            put(c, r, "TEXT TV", "NORMAL 3BD");
            CellStyles.slot(r, c, "TEXTSTYLE");
            put(c, r, "LOCATION 3BD", "TEXTDIRECTION 3BD", "ROTATION BD", "WIDTH BD", "HEIGHT BD",
                    "LINESPACINGFACTOR BD", "LINESPACINGSTYLE BS", "TEXTCOLOR CMC", "ALIGNMENT BS",
                    "FLOWDIRECTION BS", "BACKGROUNDCOLOR CMC", "BACKGROUNDSCALE BD",
                    "BACKGROUNDTRANSPARENCY BL", "BACKGROUNDFILL B", "BACKGROUNDMASK B", "COLUMNTYPE BS",
                    "AUTOHEIGHT B", "COLUMNWIDTH BD", "COLUMNGUTTER BD", "COLUMNFLOWREVERSED B");
            List<Object> sizes = new ArrayList<>();
            for (int i = r.readBitLong(); i > 0; i--) {
                sizes.add(r.readBitDouble());
            }
            c.put("COLUMNSIZES", sizes);
            put(c, r, "WORDBREAK B", "UNKNOWN B");
        } else {
            put(c, r, "HASBLOCK B");
            if (Boolean.TRUE.equals(c.get("HASBLOCK"))) {
                CellStyles.slot(r, c, "BLOCK");
                put(c, r, "BLOCKNORMAL 3BD", "BLOCKLOCATION 3BD", "BLOCKSCALE 3BD", "BLOCKROTATION BD",
                        "BLOCKCOLOR CMC");
                double[] matrix = new double[16];
                for (int i = 0; i < 16; i++) {
                    matrix[i] = r.readBitDouble();
                }
                c.put("BLOCKMATRIX", matrix);
            }
        }
        put(c, r, "BASEPOINT 3BD", "BASEDIRECTION 3BD", "BASEVERTICAL 3BD", "NORMALREVERSED B");
        if (r2010) {
            put(c, r, "TOPATTACHMENT BS", "BOTTOMATTACHMENT BS");
        }
    }

    private static List<Object[]> pairs(BitStreamReader r) {
        List<Object[]> pairs = new ArrayList<>();
        for (int i = r.readBitLong(); i > 0; i--) {
            pairs.add(new Object[] {Fields.read("3BD", r), Fields.read("3BD", r)});
        }
        return pairs;
    }

    private static void put(Map<String, Object> map, BitStreamReader r, String... specs) {
        for (String spec : specs) {
            int space = spec.indexOf(' ');
            map.put(spec.substring(0, space), Fields.read(spec.substring(space + 1), r));
        }
    }

    /** A map view that stores into the entity's values, for handle slots. */
    private static Map<String, Object> values(DwgMLeader m) {
        return new java.util.AbstractMap<String, Object>() {
            @Override
            public Object put(String key, Object value) {
                m.setValue(key, value);
                return null;
            }

            @Override
            public java.util.Set<Entry<String, Object>> entrySet() {
                return m.values().entrySet();
            }
        };
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        CellStyles.readHandleSlots(r, target.handle());
    }
}
