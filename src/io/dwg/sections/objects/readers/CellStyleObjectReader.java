package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgCellStyle;
import io.dwg.sections.objects.ObjectReader;

/**
 * CELLSTYLEMAP (a class), spec §20.4.102: number of cell styles BL, then per style the cell style
 * fields (§20.4.101.4), ID BL, class BL and name TV. Handles: those of the cell styles, in order.
 */
public class CellStyleObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.ACAD_CELLSTYLE.typeCode(); }

    @Override
    public boolean readsOwnHeader() { return false; }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        ((DwgCellStyle) target).setValue("CELLSTYLES", readNamedStyles(r, r.readBitLong()));
    }

    /** {@code count} cell styles, each followed by its ID, class and name. */
    static java.util.List<java.util.Map<String, Object>> readNamedStyles(BitStreamReader r, int count) {
        java.util.List<java.util.Map<String, Object>> styles = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            styles.add(readNamedStyle(r));
        }
        return styles;
    }

    static java.util.Map<String, Object> readNamedStyle(BitStreamReader r) {
        return readNamedStyle(r, new java.util.LinkedHashMap<>());
    }

    static java.util.Map<String, Object> readNamedStyle(BitStreamReader r, java.util.Map<String, Object> into) {
        java.util.Map<String, Object> s = CellStyles.readCellStyle(r, into);
        s.put("ID", r.readBitLong());
        s.put("CLASS", r.readBitLong());
        s.put("NAME", r.readVariableText());
        return s;
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        CellStyles.readHandleSlots(r, target.handle());
    }
}
