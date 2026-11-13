package io.dwg.render.model.entity;

/** TEXT and ATTRIB. */
public class Text extends Entity {
    public double elevation;
    public double[] insertion;                  // OCS
    public double[] alignment;                  // OCS, used when hAlign or vAlign is not 0
    public double[] extrusion;
    public double thickness;
    public double oblique;
    public double rotation;                     // radians
    public double height;
    public double widthFactor = 1.0;
    public String text;
    public int generation;
    public int hAlign;                          // 0 left, 1 center, 2 right, 3 aligned, 4 middle, 5 fit
    public int vAlign;                          // 0 baseline, 1 bottom, 2 middle, 3 top
    public boolean attribute;                   // ATTRIB : belongs to a block reference
    public String tag;
    public int attributeFlags;                  // 1 = invisible
}
