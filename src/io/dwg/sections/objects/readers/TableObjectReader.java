package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgTable;
import io.dwg.sections.objects.EntityHeaderReader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ACAD_TABLE (a class), spec §20.4.96, read into the entity's values.
 * <p>Up to R2007 (§20.4.96.1): the INSERT fields, then FLAGS BS, HORIZONTALDIRECTION 3BD,
 * COLUMNCOUNT BL, ROWCOUNT BL, the column widths and row heights (BD), then the cells row by row
 * (CELLS) and the table, border color, lineweight and visibility overrides. Handles: those of
 * INSERT, the table style, then per cell its content (text field or block), its attribute
 * definitions and its text style override, then the three row text style overrides.
 * <p>From R2010 (§20.4.96.2): the table content of §20.4.97 (CONTENT), then the break data.
 * <p>Settled on the 8 tables of the samples (R2000 to R2018), where the specification is unclear
 * or differs; with these all end their data and handles exactly:
 * <ul>
 *   <li>The R2010+ format starts with the INSERT fields, and its handles with those of INSERT,
 *       as in the older format (the table inherits INSERT); the specification lists neither.</li>
 *   <li>The colors of the old format are TC (the R2004 CMC form), as in TABLESTYLE.</li>
 *   <li>The old format's cell text is there up to R2004 in every text cell (the specification
 *       makes it depend on the field handle, which is null in all of them). In R2007 it is not
 *       there: the text is in the cell's value, whose format and value strings are the only
 *       strings of the cell (2 per cell in the R2007 table: 360 strings for 20 x 9 cells, and
 *       the value strings are those of the R2010 copy). The handles follow the
 *       specification's order, per cell.</li>
 *   <li>A cell content's format overrides follow a BL flag after its attributes.</li>
 *   <li>String values of R2007+ are sized bytes, see {@link DwgValueReader}.</li>
 * </ul>
 */
public class TableObjectReader extends InsertObjectReader {

    private static final int TEXT_CELL = 1;
    private static final int BLOCK_CELL = 2;

    @Override
    public int objectType() { return DwgObjectType.ACAD_TABLE.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);
        DwgTable table = (DwgTable) target;
        readInsertFields(table, r, v);
        if (v.from(DwgVersion.R2010)) {
            readR2010(table, r, v);
        } else {
            readOld(table, r, v);
        }
    }

    // --------------------------------------------------------------------------------------
    // until R2007
    // --------------------------------------------------------------------------------------

    private static void readOld(DwgTable t, BitStreamReader r, DwgVersion v) {
        Fields.read(t, r, "FLAGS BS", "HORIZONTALDIRECTION 3BD", "COLUMNCOUNT BL", "ROWCOUNT BL");
        int columns = (Integer) t.value("COLUMNCOUNT");
        int rows = (Integer) t.value("ROWCOUNT");
        t.setValue("COLUMNWIDTHS", doubles(r, columns));
        t.setValue("ROWHEIGHTS", doubles(r, rows));
        List<Map<String, Object>> cells = new ArrayList<>();
        List<List<String>> texts = new ArrayList<>();
        t.setValue("CELLS", cells);
        t.setValue("CELLTEXTS", texts);
        for (int row = 0; row < rows; row++) {
            List<String> rowTexts = new ArrayList<>();
            texts.add(rowTexts);
            for (int col = 0; col < columns; col++) {
                Map<String, Object> cell = readOldCell(r, v);
                cells.add(cell);
                rowTexts.add(oldCellText(cell));
            }
        }
        readTableOverrides(t, r);
    }

    private static Map<String, Object> readOldCell(BitStreamReader r, DwgVersion v) {
        Map<String, Object> c = new LinkedHashMap<>();
        put(c, r, "TYPE BS", "EDGEFLAGS RC", "MERGED B", "AUTOFIT B", "MERGEDWIDTH BL", "MERGEDHEIGHT BL",
                "ROTATION BD");
        int type = (Integer) c.get("TYPE");
        if (type == TEXT_CELL) {
            if (!v.from(DwgVersion.R2007)) {
                put(c, r, "TEXT TV");
            }
        } else if (type == BLOCK_CELL) {
            put(c, r, "BLOCKSCALE BD", "HASATTRIBUTES B");
            if (Boolean.TRUE.equals(c.get("HASATTRIBUTES"))) {
                int n = r.readBitShort();
                List<Map<String, Object>> attributes = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    Map<String, Object> a = new LinkedHashMap<>();
                    put(a, r, "INDEX BS", "TEXT TV");
                    attributes.add(a);
                }
                c.put("ATTRIBUTES", attributes);
            }
        }
        put(c, r, "HASOVERRIDES B");
        if (Boolean.TRUE.equals(c.get("HASOVERRIDES"))) {
            int flags = r.readBitLong();
            c.put("OVERRIDEFLAGS", flags);
            put(c, r, "VIRTUALEDGES RC");
            if (has(flags, 0x01)) put(c, r, "ALIGNMENT BS");
            if (has(flags, 0x02)) put(c, r, "BACKGROUNDFILLNONE B");
            if (has(flags, 0x04)) c.put("BACKGROUNDCOLOR", tc(r));
            if (has(flags, 0x08)) c.put("CONTENTCOLOR", tc(r));
            if (has(flags, 0x20)) put(c, r, "TEXTHEIGHT BD");
            String[] edges = {"TOP", "RIGHT", "BOTTOM", "LEFT"};
            for (int i = 0; i < 4; i++) {
                if (has(flags, 0x40 << i)) c.put(edges[i] + "COLOR", tc(r));
                if (has(flags, 0x400 << i)) put(c, r, edges[i] + "LINEWEIGHT BS");
                if (has(flags, 0x4000 << i)) put(c, r, edges[i] + "VISIBILITY BS");
            }
        }
        if (v.from(DwgVersion.R2007)) {
            put(c, r, "UNKNOWN BL");
            c.put("VALUE", DwgValueReader.read(r, v));
        }
        return c;
    }

    /** The cell's text: up to R2004 the text field; in R2007 the string of the cell's value. */
    @SuppressWarnings("unchecked")
    private static String oldCellText(Map<String, Object> cell) {
        Object text = cell.get("TEXT");
        if (text instanceof String && !((String) text).isEmpty()) return (String) text;
        Object value = cell.get("VALUE");
        if (value instanceof Map) {
            Object d = ((Map<String, Object>) value).get("DATA");
            if (d instanceof String) return (String) d;
        }
        return text instanceof String ? (String) text : null;
    }

    private static void readTableOverrides(DwgTable t, BitStreamReader r) {
        if (r.getInput().readBit()) {
            int flags = r.readBitLong();
            t.setValue("TABLEOVERRIDES", flags);
            if (has(flags, 0x0001)) Fields.read(t, r, "TITLESUPPRESSED B");
            if (has(flags, 0x0004)) Fields.read(t, r, "FLOWDIRECTION BS");
            if (has(flags, 0x0008)) Fields.read(t, r, "HORIZONTALMARGIN BD");
            if (has(flags, 0x0010)) Fields.read(t, r, "VERTICALMARGIN BD");
            String[] rows = {"TITLE", "HEADER", "DATA"};
            for (int i = 0; i < 3; i++) if (has(flags, 0x0020 << i)) t.setValue(rows[i] + "ROWCOLOR", tc(r));
            for (int i = 0; i < 3; i++) if (has(flags, 0x0100 << i)) Fields.read(t, r, rows[i] + "ROWFILLNONE B");
            for (int i = 0; i < 3; i++) if (has(flags, 0x0800 << i)) t.setValue(rows[i] + "ROWFILLCOLOR", tc(r));
            for (int i = 0; i < 3; i++) if (has(flags, 0x4000 << i)) Fields.read(t, r, rows[i] + "ROWALIGNMENT BS");
            for (int i = 0; i < 3; i++) if (has(flags, 0x100000 << i)) Fields.read(t, r, rows[i] + "ROWHEIGHT BD");
        }
        String[] borders = {"TITLEHORTOP", "TITLEHORINSIDE", "TITLEHORBOTTOM", "TITLEVERLEFT", "TITLEVERINSIDE",
                "TITLEVERRIGHT", "HEADERHORTOP", "HEADERHORINSIDE", "HEADERHORBOTTOM", "HEADERVERLEFT",
                "HEADERVERINSIDE", "HEADERVERRIGHT", "DATAHORTOP", "DATAHORINSIDE", "DATAHORBOTTOM",
                "DATAVERLEFT", "DATAVERINSIDE", "DATAVERRIGHT"};
        if (r.getInput().readBit()) {
            int flags = r.readBitLong();
            t.setValue("BORDERCOLOROVERRIDES", flags);
            for (int i = 0; i < borders.length; i++) if (has(flags, 1 << i)) t.setValue(borders[i] + "COLOR", tc(r));
        }
        if (r.getInput().readBit()) {
            int flags = r.readBitLong();
            t.setValue("BORDERLINEWEIGHTOVERRIDES", flags);
            for (int i = 0; i < borders.length; i++) if (has(flags, 1 << i)) Fields.read(t, r, borders[i] + "LINEWEIGHT BS");
        }
        if (r.getInput().readBit()) {
            int flags = r.readBitLong();
            t.setValue("BORDERVISIBILITYOVERRIDES", flags);
            for (int i = 0; i < borders.length; i++) if (has(flags, 1 << i)) Fields.read(t, r, borders[i] + "INVISIBLE BS");
        }
    }

    // --------------------------------------------------------------------------------------
    // R2010+
    // --------------------------------------------------------------------------------------

    private static void readR2010(DwgTable t, BitStreamReader r, DwgVersion v) {
        Fields.read(t, r, "UNKNOWN1 RC");
        CellStyles.slot(r, mapOf(t), "UNKNOWNHANDLE");
        Fields.read(t, r, "UNKNOWN2 BL");
        if (!v.from(DwgVersion.R2013)) {
            Fields.read(t, r, "UNKNOWN3 B");
        } else {
            Fields.read(t, r, "UNKNOWN4 BL");
        }
        Map<String, Object> content = new LinkedHashMap<>();
        t.setValue("CONTENT", content);
        readTableContent(content, r, v, t);
        Fields.read(t, r, "UNKNOWN5 BS", "HORIZONTALDIRECTION 3BD", "HASBREAKDATA BL");
        if (!Integer.valueOf(0).equals(t.value("HASBREAKDATA"))) {
            Map<String, Object> b = new LinkedHashMap<>();
            put(b, r, "OPTIONS BL", "FLOWDIRECTION BL", "SPACING BD", "UNKNOWN1 BL", "UNKNOWN2 BL");
            List<Map<String, Object>> positions = new ArrayList<>();
            for (int i = r.readBitLong(); i > 0; i--) {
                Map<String, Object> p = new LinkedHashMap<>();
                put(p, r, "POSITION 3BD", "HEIGHT BD", "FLAGS BL");
                positions.add(p);
            }
            b.put("POSITIONS", positions);
            t.setValue("BREAKDATA", b);
        }
        List<Map<String, Object>> ranges = new ArrayList<>();
        for (int i = r.readBitLong(); i > 0; i--) {
            Map<String, Object> p = new LinkedHashMap<>();
            put(p, r, "POSITION 3BD", "STARTROW BL", "ENDROW BL");
            ranges.add(p);
        }
        t.setValue("ROWRANGES", ranges);
    }

    /** TABLECONTENT, §20.4.97, without the common object data. */
    static void readTableContent(Map<String, Object> c, BitStreamReader r, DwgVersion v, DwgTable table) {
        put(c, r, "NAME TV", "DESCRIPTION TV");
        List<Map<String, Object>> columns = new ArrayList<>();
        c.put("COLUMNS", columns);
        for (int i = r.readBitLong(); i > 0; i--) {
            Map<String, Object> col = new LinkedHashMap<>();
            columns.add(col);
            put(col, r, "NAME TV", "CUSTOM BL");
            col.put("CUSTOMDATA", customData(r, v));
            col.put("CELLSTYLE", CellStyles.readCellStyle(r));
            put(col, r, "CELLSTYLEID BL", "WIDTH BD");
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        List<List<String>> texts = new ArrayList<>();
        c.put("ROWS", rows);
        for (int i = r.readBitLong(); i > 0; i--) {
            Map<String, Object> row = new LinkedHashMap<>();
            rows.add(row);
            List<Map<String, Object>> cells = new ArrayList<>();
            List<String> rowTexts = new ArrayList<>();
            texts.add(rowTexts);
            row.put("CELLS", cells);
            for (int j = r.readBitLong(); j > 0; j--) {
                Map<String, Object> cell = readContentCell(r, v);
                cells.add(cell);
                rowTexts.add(firstText(cell));
            }
            put(row, r, "CUSTOM BL");
            row.put("CUSTOMDATA", customData(r, v));
            row.put("CELLSTYLE", CellStyles.readCellStyle(r));
            put(row, r, "CELLSTYLEID BL", "HEIGHT BD");
        }
        List<Map<String, Object>> fields = new ArrayList<>();
        for (int i = r.readBitLong(); i > 0; i--) {
            Map<String, Object> f = new LinkedHashMap<>();
            CellStyles.slot(r, f, "FIELD");
            fields.add(f);
        }
        c.put("FIELDS", fields);
        c.put("TABLECELLSTYLE", CellStyles.readCellStyle(r));
        List<int[]> merged = new ArrayList<>();
        for (int i = r.readBitLong(); i > 0; i--) {
            merged.add(new int[] {r.readBitLong(), r.readBitLong(), r.readBitLong(), r.readBitLong()});
        }
        c.put("MERGEDRANGES", merged);
        CellStyles.slot(r, c, "TABLESTYLE");
        if (table != null) {
            table.setValue("ROWCOUNT", rows.size());
            table.setValue("COLUMNCOUNT", columns.size());
            table.setValue("CELLTEXTS", texts);
        }
    }

    private static Map<String, Object> readContentCell(BitStreamReader r, DwgVersion v) {
        Map<String, Object> cell = new LinkedHashMap<>();
        put(cell, r, "STATE BL", "TOOLTIP TV", "CUSTOM BL");
        cell.put("CUSTOMDATA", customData(r, v));
        put(cell, r, "HASLINKEDDATA BL");
        if (!Integer.valueOf(0).equals(cell.get("HASLINKEDDATA"))) {
            CellStyles.slot(r, cell, "DATALINK");
            put(cell, r, "LINKROWCOUNT BL", "LINKCOLUMNCOUNT BL", "LINKUNKNOWN BL");
        }
        List<Map<String, Object>> contents = new ArrayList<>();
        cell.put("CONTENTS", contents);
        for (int i = r.readBitLong(); i > 0; i--) {
            Map<String, Object> content = new LinkedHashMap<>();
            contents.add(content);
            int type = r.readBitLong();
            content.put("TYPE", type);
            if (type == 1) {
                content.put("VALUE", DwgValueReader.read(r, v));
            } else if (type == 2) {
                CellStyles.slot(r, content, "FIELD");
            } else if (type == 4) {
                CellStyles.slot(r, content, "BLOCK");
            }
            List<Map<String, Object>> attributes = new ArrayList<>();
            for (int j = r.readBitLong(); j > 0; j--) {
                Map<String, Object> a = new LinkedHashMap<>();
                CellStyles.slot(r, a, "ATTDEF");
                put(a, r, "VALUE TV", "INDEX BL");
                attributes.add(a);
            }
            content.put("ATTRIBUTES", attributes);
            put(content, r, "HASFORMATOVERRIDES BL");
            if (!Integer.valueOf(0).equals(content.get("HASFORMATOVERRIDES"))) {
                content.put("FORMAT", CellStyles.readContentFormat(r));
            }
        }
        cell.put("CELLSTYLE", CellStyles.readCellStyle(r));
        put(cell, r, "CELLSTYLEID BL", "UNKNOWNFLAG BL");
        if (!Integer.valueOf(0).equals(cell.get("UNKNOWNFLAG"))) {
            put(cell, r, "UNKNOWN1 BL", "UNKNOWN2 BD", "UNKNOWN3 BD", "GEOMETRYFLAGS BL");
            CellStyles.slot(r, cell, "UNKNOWNHANDLE");
            if (!Integer.valueOf(0).equals(cell.get("GEOMETRYFLAGS"))) {
                Map<String, Object> g = new LinkedHashMap<>();
                put(g, r, "TOPLEFT 3BD", "CENTER 3BD", "CONTENTWIDTH BD", "CONTENTHEIGHT BD", "WIDTH BD",
                        "HEIGHT BD", "FLAGS BL");
                cell.put("GEOMETRY", g);
            }
        }
        return cell;
    }

    /** Custom data collection, §20.4.100: BL count, then per item a name TV and a value. */
    private static Map<String, Object> customData(BitStreamReader r, DwgVersion v) {
        Map<String, Object> data = new LinkedHashMap<>();
        for (int i = r.readBitLong(); i > 0; i--) {
            String name = r.readVariableText();
            data.put(name, DwgValueReader.read(r, v));
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private static String firstText(Map<String, Object> cell) {
        for (Map<String, Object> content : (List<Map<String, Object>>) cell.get("CONTENTS")) {
            Object value = content.get("VALUE");
            if (value instanceof Map) {
                Object s = ((Map<String, Object>) value).get("VALUESTRING");
                if (s instanceof String && !((String) s).isEmpty()) return (String) s;
                Object d = ((Map<String, Object>) value).get("DATA");
                if (d instanceof String) return (String) d;
            }
        }
        return null;
    }

    // --------------------------------------------------------------------------------------
    // handles
    // --------------------------------------------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgTable t = (DwgTable) target;
        long self = target.handle();
        super.readHandles(target, r, v);
        if (v.from(DwgVersion.R2010)) {
            CellStyles.readHandleSlots(r, self);
            return;
        }
        t.setValue("TABLESTYLE", handle(r, self));
        for (Map<String, Object> cell : (List<Map<String, Object>>) t.value("CELLS")) {
            cell.put("CONTENTHANDLE", handle(r, self));
            Object attributes = cell.get("ATTRIBUTES");
            if (attributes instanceof List) {
                for (Map<String, Object> a : (List<Map<String, Object>>) attributes) {
                    a.put("ATTDEF", handle(r, self));
                }
            }
            Object flags = cell.get("OVERRIDEFLAGS");
            if (flags instanceof Integer && has((Integer) flags, 0x10)) {
                cell.put("TEXTSTYLE", handle(r, self));
            }
        }
        Object flags = t.value("TABLEOVERRIDES");
        String[] rows = {"TITLE", "HEADER", "DATA"};
        for (int i = 0; i < 3; i++) {
            if (flags instanceof Integer && has((Integer) flags, 0x20000 << i)) {
                t.setValue(rows[i] + "TEXTSTYLE", handle(r, self));
            }
        }
        // value handles (object ids) of R2007 cells
        CellStyles.readHandleSlots(r, self);
    }

    // --------------------------------------------------------------------------------------

    private static DwgHandleRef handle(BitStreamReader r, long self) {
        return r.readRef(self);
    }

    private static boolean has(int flags, int bit) {
        return (flags & bit) != 0;
    }

    private static io.dwg.core.type.CmColor tc(BitStreamReader r) {
        return Colors.of(r.readTrueColor());
    }

    private static double[] doubles(BitStreamReader r, int n) {
        double[] d = new double[n];
        for (int i = 0; i < n; i++) {
            d[i] = r.readBitDouble();
        }
        return d;
    }

    private static void put(Map<String, Object> map, BitStreamReader r, String... specs) {
        for (String spec : specs) {
            int space = spec.indexOf(' ');
            map.put(spec.substring(0, space), Fields.read(spec.substring(space + 1), r));
        }
    }

    /** A map that stores into the table's values, for handle slots. */
    private static Map<String, Object> mapOf(DwgTable t) {
        return new java.util.AbstractMap<String, Object>() {
            @Override
            public Object put(String key, Object value) {
                t.setValue(key, value);
                return null;
            }

            @Override
            public java.util.Set<Entry<String, Object>> entrySet() {
                return t.values().entrySet();
            }
        };
    }
}
