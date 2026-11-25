package io.dwg.entities;

import java.util.Map;

/**
 * An object that keeps (some of) its fields under names, in file order: the names follow the
 * specification's tables (or the DXF variables). Points are {@link io.dwg.core.type.Point3D}
 * (2D points with Z 0), colors {@link io.dwg.core.type.CmColor}, handles
 * {@link io.dwg.core.type.DwgHandleRef}.
 */
public interface ValueHolder {

    /** All named fields read from the file. */
    Map<String, Object> values();

    /** The value of a field, or {@code null} when the file does not hold it. */
    default Object value(String name) { return values().get(name); }

    void setValue(String name, Object value);
}
