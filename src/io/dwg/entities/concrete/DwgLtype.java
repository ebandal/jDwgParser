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
    public static final class Dash {
        private final double length;
        private final int shapeCode;
        private final double xOffset;
        private final double yOffset;
        private final double scale;
        private final double rotation;
        private final int shapeFlag;
        private final String text;
        private final DwgHandleRef styleHandle;

        public Dash(double length, int shapeCode, double xOffset, double yOffset, double scale, double rotation, int shapeFlag, String text, DwgHandleRef styleHandle) {
            this.length = length;
            this.shapeCode = shapeCode;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.scale = scale;
            this.rotation = rotation;
            this.shapeFlag = shapeFlag;
            this.text = text;
            this.styleHandle = styleHandle;
        }

        public double length() {
            return length;
        }

        public int shapeCode() {
            return shapeCode;
        }

        public double xOffset() {
            return xOffset;
        }

        public double yOffset() {
            return yOffset;
        }

        public double scale() {
            return scale;
        }

        public double rotation() {
            return rotation;
        }

        public int shapeFlag() {
            return shapeFlag;
        }

        public String text() {
            return text;
        }

        public DwgHandleRef styleHandle() {
            return styleHandle;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Dash)) return false;
            Dash other = (Dash) o;
            return Double.compare(length, other.length) == 0
                    && shapeCode == other.shapeCode
                    && Double.compare(xOffset, other.xOffset) == 0
                    && Double.compare(yOffset, other.yOffset) == 0
                    && Double.compare(scale, other.scale) == 0
                    && Double.compare(rotation, other.rotation) == 0
                    && shapeFlag == other.shapeFlag
                    && java.util.Objects.equals(text, other.text)
                    && java.util.Objects.equals(styleHandle, other.styleHandle);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(length, shapeCode, xOffset, yOffset, scale, rotation, shapeFlag, text, styleHandle);
        }

        @Override
        public String toString() {
            return "Dash[" + "length=" + length + ", " + "shapeCode=" + shapeCode + ", " + "xOffset=" + xOffset + ", " + "yOffset=" + yOffset + ", " + "scale=" + scale + ", " + "rotation=" + rotation + ", " + "shapeFlag=" + shapeFlag + ", " + "text=" + text + ", " + "styleHandle=" + styleHandle + "]";
        }

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
