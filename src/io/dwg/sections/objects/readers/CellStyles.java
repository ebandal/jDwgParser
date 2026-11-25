package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.sections.objects.ObjectHeaderData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The content format (spec §20.4.101.3) and cell style (§20.4.101.4) of TABLESTYLE, CELLSTYLEMAP
 * and the table objects, read into maps under the DXF group codes' meaning. Colors are TC: the
 * R2004+ CMC form in every version. The handles they refer to are in the handle stream; they are
 * registered as slots ({@link ObjectHeaderData#handleSlots}) and filled by {@link #readHandleSlots}.
 */
final class CellStyles {

    private static final int MARGIN_OVERRIDES = 1;

    private CellStyles() {
    }

    static Map<String, Object> readCellStyle(BitStreamReader r) {
        return readCellStyle(r, new LinkedHashMap<>());
    }

    /** Reads the cell style fields into {@code s}. */
    static Map<String, Object> readCellStyle(BitStreamReader r, Map<String, Object> s) {
        s.put("TYPE", r.readBitLong());
        int data = r.readBitShort();
        s.put("HASDATA", data);
        if (data == 0) {
            return s;
        }
        s.put("OVERRIDES", r.readBitLong());
        s.put("MERGEFLAGS", r.readBitLong());
        s.put("BACKGROUNDCOLOR", Colors.of(r.readTrueColor()));
        s.put("CONTENTLAYOUT", r.readBitLong());
        s.put("CONTENTFORMAT", readContentFormat(r));
        int marginFlags = r.readBitShort();
        s.put("MARGINFLAGS", marginFlags);
        if ((marginFlags & MARGIN_OVERRIDES) != 0) {
            String[] names = {"MARGINVERTICAL", "MARGINHORIZONTAL", "MARGINBOTTOM", "MARGINRIGHT",
                              "MARGINHSPACING", "MARGINVSPACING"};
            for (String name : names) {
                s.put(name, r.readBitDouble());
            }
        }
        int borders = r.readBitLong();
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < borders; i++) {
            Map<String, Object> b = new LinkedHashMap<>();
            int edges = r.readBitLong();
            b.put("EDGES", edges);
            if (edges != 0) {
                b.put("OVERRIDES", r.readBitLong());
                b.put("BORDERTYPE", r.readBitLong());
                b.put("COLOR", Colors.of(r.readTrueColor()));
                b.put("LINEWEIGHT", r.readBitLong());
                slot(r, b, "LINETYPE");
                b.put("INVISIBLE", r.readBitLong());
                b.put("DOUBLESPACING", r.readBitDouble());
            }
            list.add(b);
        }
        s.put("BORDERS", list);
        return s;
    }

    static Map<String, Object> readContentFormat(BitStreamReader r) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("OVERRIDES", r.readBitLong());
        f.put("PROPERTYFLAGS", r.readBitLong());
        f.put("VALUETYPE", r.readBitLong());
        f.put("VALUEUNITS", r.readBitLong());
        f.put("VALUEFORMAT", r.readVariableText());
        f.put("ROTATION", r.readBitDouble());
        f.put("BLOCKSCALE", r.readBitDouble());
        f.put("ALIGNMENT", r.readBitLong());
        f.put("COLOR", Colors.of(r.readTrueColor()));
        slot(r, f, "TEXTSTYLE");
        f.put("TEXTHEIGHT", r.readBitDouble());
        return f;
    }

    /** Registers a handle of the handle stream that belongs under {@code name} in {@code map}. */
    static void slot(BitStreamReader r, Map<String, Object> map, String name) {
        map.put(name, null);
        ObjectHeaderData.of(r).handleSlots.add(h -> map.put(name, h));
    }

    /** Fills the registered slots from the handle stream, in order. */
    static void readHandleSlots(BitStreamReader r, long self) {
        for (java.util.function.Consumer<DwgHandleRef> slot : ObjectHeaderData.of(r).handleSlots) {
            slot.accept(new DwgHandleRef(r.readHandleReference(self)));
        }
    }
}
