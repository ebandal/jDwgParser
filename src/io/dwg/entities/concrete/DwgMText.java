package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * MTEXT 엔티티 (타입 0x2C)
 * 여러 줄 텍스트 (포맷, 색상, 글꼴 지원)
 */
public class DwgMText extends AbstractDwgEntity {
    private Point3D location;
    private double width;  // 텍스트 박스 폭
    private double height; // 텍스트 박스 높이
    private String text;
    private int attachmentPoint;  // 정렬 방식 (1-9)
    private String styleName;
    private double angle;
    private double[] extrusion = io.dwg.entities.Extrusions.shared();

    @Override
    public DwgObjectType objectType() { return DwgObjectType.MTEXT; }

    public Point3D location() { return location; }
    public double width() { return width; }
    public double height() { return height; }
    public String text() { return text; }
    public int attachmentPoint() { return attachmentPoint; }
    public String styleName() { return styleName; }
    public double angle() { return angle; }
    public double[] extrusion() { return io.dwg.entities.Extrusions.copy(extrusion); }

    public void setLocation(Point3D location) { this.location = location; }
    public void setWidth(double width) { this.width = width; }
    public void setHeight(double height) { this.height = height; }
    public void setText(String text) { this.text = text; }
    public void setAttachmentPoint(int attachmentPoint) { this.attachmentPoint = attachmentPoint; }
    public void setStyleName(String styleName) { this.styleName = styleName; }
    public void setAngle(double angle) { this.angle = angle; }
    public void setExtrusion(double[] extrusion) { this.extrusion = io.dwg.entities.Extrusions.share(extrusion); }

    private Point3D xAxisDirection;     // 문자 진행 방향 (WCS)

    public Point3D xAxisDirection() { return xAxisDirection; }
    public void setXAxisDirection(Point3D xAxisDirection) { this.xAxisDirection = xAxisDirection; }

    // spec §20.4.46
    private io.dwg.core.type.DwgHandleRef styleHandle;
    private double rectangleHeight;
    private int drawingDirection;
    private int lineSpacingStyle;
    private double lineSpacingFactor = 1.0;
    private int backgroundFlags;
    private double backgroundScale = 1.5;
    private io.dwg.core.type.CmColor backgroundColor;
    private int backgroundTransparency;
    private int columnType;
    private int columnCount;
    private double columnWidth;
    private double columnGutter;
    private boolean columnAutoHeight;
    private boolean columnFlowReversed;
    private final java.util.List<Double> columnHeights = new java.util.ArrayList<>();
    private boolean notAnnotative;

    /** R2018+: the MTEXT is not annotative; its redundant fields and columns are in the file. */
    public boolean isNotAnnotative() { return notAnnotative; }
    public void setNotAnnotative(boolean b) { this.notAnnotative = b; }

    /** The STYLE record (DXF 7). */
    public io.dwg.core.type.DwgHandleRef styleHandle() { return styleHandle; }
    /** R2007+: the height of the reference rectangle (DXF 46). */
    public double rectangleHeight() { return rectangleHeight; }
    /** DXF 72: 1 left to right, 3 top to bottom, 5 by style. */
    public int drawingDirection() { return drawingDirection; }
    /** R2000+ (DXF 73) */
    public int lineSpacingStyle() { return lineSpacingStyle; }
    /** R2000+ (DXF 44) */
    public double lineSpacingFactor() { return lineSpacingFactor; }
    /** R2004+ (DXF 90): 1 background fill, 2 fill with the drawing color, 0x10 text frame (R2018+). */
    public int backgroundFlags() { return backgroundFlags; }
    /** R2004+, with a background (DXF 45). */
    public double backgroundScale() { return backgroundScale; }
    /** R2004+, with a background (DXF 63); {@code null} without one. */
    public io.dwg.core.type.CmColor backgroundColor() { return backgroundColor; }
    /** R2004+, with a background (DXF 441). */
    public int backgroundTransparency() { return backgroundTransparency; }
    /** R2018+: 0 no columns, 1 static, 2 dynamic (DXF 71). */
    public int columnType() { return columnType; }
    public int columnCount() { return columnCount; }
    public double columnWidth() { return columnWidth; }
    public double columnGutter() { return columnGutter; }
    public boolean isColumnAutoHeight() { return columnAutoHeight; }
    public boolean isColumnFlowReversed() { return columnFlowReversed; }
    public java.util.List<Double> columnHeights() { return java.util.Collections.unmodifiableList(columnHeights); }

    public void setStyleHandle(io.dwg.core.type.DwgHandleRef h) { this.styleHandle = h; }
    public void setRectangleHeight(double h) { this.rectangleHeight = h; }
    public void setDrawingDirection(int d) { this.drawingDirection = d; }
    public void setLineSpacingStyle(int s) { this.lineSpacingStyle = s; }
    public void setLineSpacingFactor(double f) { this.lineSpacingFactor = f; }
    public void setBackgroundFlags(int f) { this.backgroundFlags = f; }
    public void setBackgroundScale(double s) { this.backgroundScale = s; }
    public void setBackgroundColor(io.dwg.core.type.CmColor c) { this.backgroundColor = c; }
    public void setBackgroundTransparency(int t) { this.backgroundTransparency = t; }
    public void setColumnType(int t) { this.columnType = t; }
    public void setColumnCount(int n) { this.columnCount = n; }
    public void setColumnWidth(double w) { this.columnWidth = w; }
    public void setColumnGutter(double g) { this.columnGutter = g; }
    public void setColumnAutoHeight(boolean b) { this.columnAutoHeight = b; }
    public void setColumnFlowReversed(boolean b) { this.columnFlowReversed = b; }
    public void addColumnHeight(double h) { this.columnHeights.add(h); }
}
