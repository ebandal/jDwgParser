package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * MULTILEADER (a class, spec §20.4.48): leaders with a text or block content. All fields are in
 * {@link #values()} under the names of {@code MLeaderObjectReader}; the context (§20.4.86) is the
 * map CONTEXT, with the leader roots (ROOTS) and their lines (LINES, each with POINTS).
 */
public class DwgMLeader extends AbstractDwgEntity {

    @Override
    public DwgObjectType objectType() { return DwgObjectType.MULTILEADER; }

    /** The context data: content, leader roots and lines. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> context() {
        Object c = value("CONTEXT");
        return c instanceof Map ? (Map<String, Object>) c : Collections.emptyMap();
    }

    /** The leader roots of the context. */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> leaderRoots() {
        Object r = context().get("ROOTS");
        return r instanceof List ? (List<Map<String, Object>>) r : Collections.emptyList();
    }

    /** The text of the content, or {@code null} when the content is not text. */
    public String text() {
        Object t = context().get("TEXT");
        return t instanceof String ? (String) t : null;
    }
}
