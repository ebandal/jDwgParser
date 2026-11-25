package io.dwg.entities.concrete;

import io.dwg.core.type.DwgHandleRef;
import io.dwg.entities.AbstractDwgTableRecord;
import io.dwg.entities.DwgObjectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * LTYPE table record (type 57, spec §20.4.58): a linetype and its dashes.
 */
public class DwgLtype extends AbstractDwgTableRecord {

    /** Shape flag bit: the text or shape is not rotated with the line. */
    public static final int SHAPE_UPRIGHT = 1;
    /** Shape flag bit: the element is text; {@link Dash#text} holds it. */
    public static final int SHAPE_IS_TEXT = 2;
    /** Shape flag bit: the element is a shape; {@link Dash#shapeCode} is its number. */
    public static final int SHAPE_IS_SHAPE = 4;

    /**
     * One element of the pattern: a dash (length &gt; 0), a gap (&lt; 0) or a dot (0), with a shape
     * or a text when {@code shapeFlag} says so. {@code styleHandle} is the STYLE record of the
     * shape or text.
     */
    public record Dash(double length, int shapeCode, double xOffset, double yOffset, double scale,
                       double rotation, int shapeFlag, String text, DwgHandleRef styleHandle) {
        /** This dash with the style handle filled in. */
        public Dash withStyleHandle(DwgHandleRef handle) {
            return new Dash(length, shapeCode, xOffset, yOffset, scale, rotation, shapeFlag, text, handle);
        }
    }

    private String description = "";
    private double patternLength;
    private int alignment = 'A';
    private final List<Dash> dashes = new ArrayList<>();
    private byte[] stringArea;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.LTYPE; }

    public String description() { return description; }
    /** The length of one repetition of the pattern (DXF 40). */
    public double patternLength() { return patternLength; }
    /** Same as {@link #patternLength()}. */
    public double totalLength() { return patternLength; }
    /** Always 'A'. */
    public int alignment() { return alignment; }
    public List<Dash> dashes() { return Collections.unmodifiableList(dashes); }
    public int numDashes() { return dashes.size(); }
    /** The text area the text elements point into, as stored; {@code null} if the file has none. */
    public byte[] stringArea() { return stringArea; }

    public void setDescription(String description) { this.description = description == null ? "" : description; }
    public void setPatternLength(double length) { this.patternLength = length; }
    public void setAlignment(int alignment) { this.alignment = alignment; }
    public void addDash(Dash dash) { dashes.add(dash); }
    public void setDash(int index, Dash dash) { dashes.set(index, dash); }
    public void setStringArea(byte[] area) { this.stringArea = area; }
}
