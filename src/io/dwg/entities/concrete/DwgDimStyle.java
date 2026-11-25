package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgTableRecord;
import io.dwg.entities.DwgObjectType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DIMSTYLE table record (type 69, spec §20.4.68). The dimension variables are kept under their
 * system variable names (DIMSCALE, DIMTXT, ...), in file order, with these value types: Boolean
 * (B), Integer (RC, BS), Double (BD), String (T, TV), {@link io.dwg.core.type.CmColor} (CMC, and
 * the colors DIMCLRD, DIMCLRE, DIMCLRT in every version) and
 * {@link io.dwg.core.type.DwgHandleRef} for the handles (DIMTXSTY, and from R2000 DIMLDRBLK,
 * DIMBLK, DIMBLK1, DIMBLK2; from R2007 DIMLTYPE, DIMLTEX1, DIMLTEX2). R13–R14 files name the
 * arrow blocks DIMBLK, DIMBLK1, DIMBLK2 with strings instead.
 */
public class DwgDimStyle extends AbstractDwgTableRecord {
    private final Map<String, Object> variables = new LinkedHashMap<>();

    @Override
    public DwgObjectType objectType() { return DwgObjectType.DIMSTYLE; }

    /** All dimension variables read from the file. */
    public Map<String, Object> variables() { return Collections.unmodifiableMap(variables); }

    /** The value of one variable, or {@code null} when the file does not hold it. */
    public Object get(String name) { return variables.get(name); }

    public void set(String name, Object value) { variables.put(name, value); }

    /** DIMTXT */
    public double textHeight() { return number("DIMTXT", 0.18); }
    /** DIMGAP */
    public double textGap() { return number("DIMGAP", 0.09); }
    /** DIMASZ */
    public double arrowSize() { return number("DIMASZ", 0.18); }
    /** DIMEXE */
    public double lineExtension() { return number("DIMEXE", 0.18); }
    /** DIMEXO */
    public double lineOffset() { return number("DIMEXO", 0.0625); }
    /** DIMSCALE */
    public double scale() { return number("DIMSCALE", 1.0); }

    private double number(String name, double otherwise) {
        Object value = variables.get(name);
        return value instanceof Number ? ((Number) value).doubleValue() : otherwise;
    }
}
