package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgTableRecord;
import io.dwg.entities.DwgObjectType;

/**
 * STYLE table record (type 53, SHAPEFILE in spec §20.4.56): a text style or a shape file.
 * A TrueType font is often named in the record's extended data instead of
 * {@link #fontFilename()} (spec §20.4.56).
 */
public class DwgStyle extends AbstractDwgTableRecord {

    /** Generation flag: text is backward (mirrored in X). */
    public static final int BACKWARD = 2;
    /** Generation flag: text is upside down (mirrored in Y). */
    public static final int UPSIDE_DOWN = 4;

    private boolean vertical;
    private boolean shapeFile;
    private double fixedHeight;
    private double width = 1.0;
    private double oblique;
    private int generation;
    private double lastHeight;
    private String fontFilename = "";
    private String bigFontFilename = "";

    @Override
    public DwgObjectType objectType() { return DwgObjectType.STYLE; }

    public boolean isVertical() { return vertical; }
    /** The record is a shape file, not a text style. */
    public boolean isShapeFile() { return shapeFile; }
    /** 0 when the height is not fixed (DXF 40). */
    public double fixedHeight() { return fixedHeight; }
    /** The width factor (DXF 41). */
    public double width() { return width; }
    /** The oblique angle in radians (DXF 50). */
    public double oblique() { return oblique; }
    /** {@link #BACKWARD} and {@link #UPSIDE_DOWN} (DXF 71). */
    public int generation() { return generation; }
    public double lastHeight() { return lastHeight; }
    public String fontFilename() { return fontFilename; }
    public String bigFontFilename() { return bigFontFilename; }
    /** The flags as DXF group 70 holds them: 1 shape file, 4 vertical text. */
    public int flags() { return (shapeFile ? 1 : 0) | (vertical ? 4 : 0); }

    public void setVertical(boolean vertical) { this.vertical = vertical; }
    public void setShapeFile(boolean shapeFile) { this.shapeFile = shapeFile; }
    public void setFixedHeight(double height) { this.fixedHeight = height; }
    public void setWidth(double width) { this.width = width; }
    public void setOblique(double oblique) { this.oblique = oblique; }
    public void setGeneration(int generation) { this.generation = generation; }
    public void setLastHeight(double height) { this.lastHeight = height; }
    public void setFontFilename(String filename) { this.fontFilename = filename == null ? "" : filename; }
    public void setBigFontFilename(String filename) { this.bigFontFilename = filename == null ? "" : filename; }
}
