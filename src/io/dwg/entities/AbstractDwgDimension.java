package io.dwg.entities;

import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point3D;

/**
 * The fields every DIMENSION has (spec §20.4.22, common dimension data) and the points of the
 * seven dimension types (§20.4.23–§20.4.29). Each type uses only some of the points; the others
 * stay {@code null}. The point names follow their DXF group codes (10, 13, 14, 15, 16).
 * The drawing of a dimension is in its anonymous block, {@link #blockHandle()}.
 */
public abstract class AbstractDwgDimension extends AbstractDwgEntity implements DwgDimension {
    private int dimensionVersion;
    private double[] extrusion = io.dwg.entities.Extrusions.shared();
    private Point3D textMidpoint;
    private int flags1;
    private String text = "";
    private double textRotation;
    private double horizontalDirection;
    private Point3D insertionScale = new Point3D(1, 1, 1);
    private double insertionRotation;
    private int attachmentPoint;
    private int lineSpacingStyle;
    private double lineSpacingFactor;
    private double actualMeasurement;
    private boolean flipArrow1;
    private boolean flipArrow2;
    private Point3D point12;

    private Point3D point10;
    private Point3D point13;
    private Point3D point14;
    private Point3D point15;
    private Point3D point16;
    private double extensionLineRotation;
    private double dimensionRotation;
    private double leaderLength;
    private int flags2;

    private DwgHandleRef dimStyleHandle;
    private DwgHandleRef blockHandle;

    /** R2010+: the dimension's version (DXF 280, 0 = R2010). */
    public int dimensionVersion() { return dimensionVersion; }
    public double[] extrusion() { return io.dwg.entities.Extrusions.copy(extrusion); }
    /** The middle of the text (DXF 11); its Z is the dimension's elevation. */
    public Point3D textMidpoint() { return textMidpoint; }
    /** Flags 1: bit 0 is the opposite of bit 128 of DXF 70, bit 1 is bit 32 of DXF 70. */
    public int flags1() { return flags1; }
    /** The user text (DXF 1); empty when the measurement is shown. */
    public String text() { return text; }
    public double textRotation() { return textRotation; }
    public double horizontalDirection() { return horizontalDirection; }
    /** The scale of the anonymous block's insertion. */
    public Point3D insertionScales() { return insertionScale; }
    /** The rotation of the anonymous block's insertion. */
    public double insertionRotation() { return insertionRotation; }
    /** R2000+ (DXF 71) */
    public int attachmentPoint() { return attachmentPoint; }
    /** R2000+ (DXF 72) */
    public int lineSpacingStyle() { return lineSpacingStyle; }
    /** R2000+ (DXF 41) */
    public double lineSpacingFactor() { return lineSpacingFactor; }
    /** R2000+: the measured value (DXF 42). */
    public double actualMeasurement() { return actualMeasurement; }
    /** R2007+ (DXF 74) */
    public boolean flipArrow1() { return flipArrow1; }
    /** R2007+ (DXF 75) */
    public boolean flipArrow2() { return flipArrow2; }
    /** DXF 12, with the elevation as Z. */
    public Point3D point12() { return point12; }

    /** DXF 10: the definition point. */
    public Point3D point10() { return point10; }
    public Point3D point13() { return point13; }
    public Point3D point14() { return point14; }
    public Point3D point15() { return point15; }
    /** ANG_2LN only: DXF 16, with the elevation as Z. */
    public Point3D point16() { return point16; }
    /** LINEAR and ALIGNED: the extension line rotation (DXF 52). */
    public double extensionLineRotation() { return extensionLineRotation; }
    /** LINEAR: the dimension line rotation (DXF 50). */
    public double dimensionRotation() { return dimensionRotation; }
    /** RADIUS and DIAMETER (DXF 40) */
    public double leaderLength() { return leaderLength; }
    /** ORDINATE: flags 2; bit 0 is bit 64 of DXF 70 (X type). */
    public int flags2() { return flags2; }

    @Override public DwgHandleRef dimStyleHandle() { return dimStyleHandle; }
    @Override public DwgHandleRef blockHandle() { return blockHandle; }
    @Override public void setDimStyleHandle(DwgHandleRef h) { this.dimStyleHandle = h; }
    @Override public void setBlockHandle(DwgHandleRef h) { this.blockHandle = h; }

    // names of the first version of this model
    /** Same as {@link #point10()}. */
    public Point3D definitionPoint() { return point10; }
    /** Same as {@link #textMidpoint()}. */
    public Point3D midpointOfText() { return textMidpoint; }
    /** The X scale of {@link #insertionScales()}. */
    public double insertionScale() { return insertionScale.x(); }
    /** The file holds no style name, only {@link #dimStyleHandle()}; always "". */
    public String dimensionStyleName() { return ""; }

    public void setDimensionVersion(int v) { this.dimensionVersion = v; }
    public void setExtrusion(double[] e) { this.extrusion = io.dwg.entities.Extrusions.share(e); }
    public void setTextMidpoint(Point3D p) { this.textMidpoint = p; }
    public void setFlags1(int f) { this.flags1 = f; }
    public void setText(String t) { this.text = t == null ? "" : t; }
    public void setTextRotation(double a) { this.textRotation = a; }
    public void setHorizontalDirection(double a) { this.horizontalDirection = a; }
    public void setInsertionScales(Point3D s) { this.insertionScale = s; }
    public void setInsertionRotation(double a) { this.insertionRotation = a; }
    public void setAttachmentPoint(int a) { this.attachmentPoint = a; }
    public void setLineSpacingStyle(int s) { this.lineSpacingStyle = s; }
    public void setLineSpacingFactor(double f) { this.lineSpacingFactor = f; }
    public void setActualMeasurement(double m) { this.actualMeasurement = m; }
    public void setFlipArrow1(boolean f) { this.flipArrow1 = f; }
    public void setFlipArrow2(boolean f) { this.flipArrow2 = f; }
    public void setPoint12(Point3D p) { this.point12 = p; }
    public void setPoint10(Point3D p) { this.point10 = p; }
    public void setPoint13(Point3D p) { this.point13 = p; }
    public void setPoint14(Point3D p) { this.point14 = p; }
    public void setPoint15(Point3D p) { this.point15 = p; }
    public void setPoint16(Point3D p) { this.point16 = p; }
    public void setExtensionLineRotation(double a) { this.extensionLineRotation = a; }
    public void setDimensionRotation(double a) { this.dimensionRotation = a; }
    public void setLeaderLength(double l) { this.leaderLength = l; }
    public void setFlags2(int f) { this.flags2 = f; }
}
