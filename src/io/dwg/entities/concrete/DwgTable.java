package io.dwg.entities.concrete;

import io.dwg.entities.DwgObjectType;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * ACAD_TABLE (a class, spec §20.4.96): an INSERT of an anonymous block ("*T...") that holds the
 * drawing of the table, and the table data. The data is in {@link #values()} under the names of
 * {@code TableObjectReader}; up to R2007 the cells are CELLS (row by row), from R2010 the table
 * content is CONTENT (§20.4.97) with its ROWS and their CELLS.
 */
public class DwgTable extends DwgInsert {

    @Override
    public DwgObjectType objectType() { return DwgObjectType.ACAD_TABLE; }

    public int rowCount() { return number("ROWCOUNT"); }
    public int columnCount() { return number("COLUMNCOUNT"); }

    /**
     * The text of each cell, row by row; {@code null} for a cell without text. Up to R2007 the
     * cell text; from R2010 the string value of the cell's first content.
     */
    @SuppressWarnings("unchecked")
    public List<List<String>> cellTexts() {
        Object t = value("CELLTEXTS");
        return t instanceof List ? (List<List<String>>) t : Collections.emptyList();
    }

    /** The table content (R2010+), or an empty map. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> content() {
        Object c = value("CONTENT");
        return c instanceof Map ? (Map<String, Object>) c : Collections.emptyMap();
    }

    private int number(String name) {
        Object v = value(name);
        return v instanceof Integer ? (Integer) v : 0;
    }
}
