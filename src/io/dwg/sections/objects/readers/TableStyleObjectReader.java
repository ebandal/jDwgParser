package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgTableStyle;
import io.dwg.sections.objects.ObjectReader;

/**
 * TABLESTYLE (a class), spec §20.4.101, kept in the object's values.
 * <pre>
 *   until R2007: DESCRIPTION TV, FLOWDIRECTION BS, FLAGS BS, HMARGIN BD, VMARGIN BD,
 *     SUPPRESSTITLE B, SUPPRESSHEADER B, then ROWSTYLES, three maps (data, title, header):
 *     TEXTSTYLE (handle), TEXTHEIGHT BD, ALIGNMENT BS, TEXTCOLOR TC, FILLCOLOR TC,
 *     BACKGROUNDENABLED B, BORDERS (six maps: LINEWEIGHT BS, VISIBLE B, COLOR TC),
 *     R2007 DATATYPE BL, UNITTYPE BL, FORMAT TV
 *   R2010+: unknown RC, DESCRIPTION TV, unknown BL, unknown BL, a handle (UNKNOWN),
 *     TABLECELLSTYLE (cell style, ID, class, name), CELLSTYLES (BL count, then per style an
 *     unknown BL, the cell style, ID, class, name)
 *   handles: (parent, reactors, xdictionary,) the handles of the fields above, in order
 * </pre>
 * Two points differ from the specification, settled on the samples: the colors of the old
 * format are TC (the R2004 CMC form) in every version, as the table data written by newer
 * AutoCAD into R13–R2000 files has them; and from R2010 each style of the list starts with an
 * unknown BL (1 in the samples). With both, all TABLESTYLEs of the samples end exactly.
 */
public class TableStyleObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.ACAD_TABLESTYLE.typeCode(); }

    @Override
    public boolean readsOwnHeader() { return false; }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgTableStyle style = (DwgTableStyle) target;
        if (v.from(DwgVersion.R2010)) {
            r.readRawChar();                       // unknown
            Fields.read(style, r, "DESCRIPTION TV", "UNKNOWN1 BL", "UNKNOWN2 BL");
            java.util.Map<String, Object> unknown = new java.util.LinkedHashMap<>();
            CellStyles.slot(r, unknown, "HANDLE");
            style.setValue("UNKNOWN", unknown);
            style.setValue("TABLECELLSTYLE", CellStyleObjectReader.readNamedStyle(r));
            int count = r.readBitLong();
            java.util.List<java.util.Map<String, Object>> styles = new java.util.ArrayList<>();
            style.setValue("CELLSTYLES", styles);
            for (int i = 0; i < count; i++) {
                java.util.Map<String, Object> st = new java.util.LinkedHashMap<>();
                styles.add(st);
                st.put("UNKNOWN", r.readBitLong());
                CellStyleObjectReader.readNamedStyle(r, st);
            }
            return;
        }
        Fields.read(style, r, "DESCRIPTION TV", "FLOWDIRECTION BS", "FLAGS BS", "HMARGIN BD", "VMARGIN BD",
                "SUPPRESSTITLE B", "SUPPRESSHEADER B");
        java.util.List<java.util.Map<String, Object>> rows = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            CellStyles.slot(r, row, "TEXTSTYLE");
            row.put("TEXTHEIGHT", r.readBitDouble());
            row.put("ALIGNMENT", r.readBitShort());
            row.put("TEXTCOLOR", Colors.of(r.readTrueColor()));
            row.put("FILLCOLOR", Colors.of(r.readTrueColor()));
            row.put("BACKGROUNDENABLED", r.getInput().readBit());
            java.util.List<java.util.Map<String, Object>> borders = new java.util.ArrayList<>();
            for (int j = 0; j < 6; j++) {
                java.util.Map<String, Object> b = new java.util.LinkedHashMap<>();
                b.put("LINEWEIGHT", r.readBitShort());
                b.put("VISIBLE", r.getInput().readBit());
                b.put("COLOR", Colors.of(r.readTrueColor()));
                borders.add(b);
            }
            row.put("BORDERS", borders);
            if (v.from(DwgVersion.R2007)) {
                row.put("DATATYPE", r.readBitLong());
                row.put("UNITTYPE", r.readBitLong());
                row.put("FORMAT", r.readVariableText());
            }
            rows.add(row);
        }
        style.setValue("ROWSTYLES", rows);
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        CellStyles.readHandleSlots(r, target.handle());
    }
}
