package io.dwg.entities.concrete;

import io.dwg.core.type.DwgHandleRef;
import io.dwg.entities.AbstractDwgObject;
import io.dwg.entities.DwgNonEntityObject;
import io.dwg.entities.DwgObjectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * FIELD (a class, spec §20.4.76): a field of a text, with its code and its last value. All fields
 * are in {@link #values()} under the names of {@code FieldObjectReader}; the value is a map, see
 * {@code DwgValueReader}.
 */
public class DwgField extends AbstractDwgObject implements DwgNonEntityObject {
    private final List<DwgHandleRef> childFieldHandles = new ArrayList<>();
    private final List<DwgHandleRef> objectHandles = new ArrayList<>();

    @Override
    public DwgObjectType objectType() { return DwgObjectType.ACAD_FIELD; }

    @Override
    public boolean isEntity() { return false; }

    /** The field code, e.g. "%<\AcVar Date>%" (DXF 2, 3). */
    public String fieldExpression() { return text("CODE"); }
    /** The value as text (DXF 301). */
    public String fieldValue() { return text("VALUETEXT"); }
    /** Evaluation status flags (DXF 95). */
    public int evaluationStatus() { return value("STATUS") instanceof Integer ? (Integer) value("STATUS") : 0; }
    /** R13–R2004: the format string (DXF 4). */
    public String fieldFormat() { return text("FORMAT"); }
    /** The value (see {@code DwgValueReader}). */
    @SuppressWarnings("unchecked")
    public Map<String, Object> fieldData() { return (Map<String, Object>) value("VALUE"); }
    public List<DwgHandleRef> childFieldHandles() { return Collections.unmodifiableList(childFieldHandles); }
    public List<DwgHandleRef> objectHandles() { return Collections.unmodifiableList(objectHandles); }

    public void addChildFieldHandle(DwgHandleRef h) { childFieldHandles.add(h); }
    public void addObjectHandle(DwgHandleRef h) { objectHandles.add(h); }

    private String text(String name) {
        return value(name) instanceof String ? (String) value(name) : "";
    }
}
