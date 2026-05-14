package structure.entities;

public class DwgText extends AbstractDwgEntity {
    private double dataFlags;
    private double elevation;
    private Point2D insertionPoint;
    private Point2D alignmentPoint;
    private double[] extrusion = {0.0, 0.0, 1.0};
    private double thickness;
    private double obliquAngle;
    private double rotationAngle;
    private double height;
    private double widthFactor;
    private String value = "";
    private int generation;
    private int horizontalAlignment;
    private int verticalAlignment;
    private DwgHandleRef styleHandle;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.TEXT; }

    public Point2D insertionPoint() { return insertionPoint; }
    public Point2D alignmentPoint() { return alignmentPoint; }
    public double height() { return height; }
    public double widthFactor() { return widthFactor; }
    public String value() { return value; }
    public double rotationAngle() { return rotationAngle; }
    public int horizontalAlignment() { return horizontalAlignment; }
    public int verticalAlignment() { return verticalAlignment; }
    public double obliquAngle() { return obliquAngle; }
    public double thickness() { return thickness; }
    public double[] extrusion() { return extrusion; }
    public int generation() { return generation; }
    public DwgHandleRef styleHandle() { return styleHandle; }
    public double dataFlags() { return dataFlags; }
    public double elevation() { return elevation; }

    public void setInsertionPoint(Point2D p) { this.insertionPoint = p; }
    public void setAlignmentPoint(Point2D p) { this.alignmentPoint = p; }
    public void setHeight(double height) { this.height = height; }
    public void setWidthFactor(double widthFactor) { this.widthFactor = widthFactor; }
    public void setValue(String value) { this.value = value; }
    public void setRotationAngle(double rotationAngle) { this.rotationAngle = rotationAngle; }
    public void setHorizontalAlignment(int h) { this.horizontalAlignment = h; }
    public void setVerticalAlignment(int v) { this.verticalAlignment = v; }
    public void setObliquAngle(double obliquAngle) { this.obliquAngle = obliquAngle; }
    public void setThickness(double thickness) { this.thickness = thickness; }
    public void setExtrusion(double[] extrusion) { this.extrusion = extrusion; }
    public void setGeneration(int generation) { this.generation = generation; }
    public void setStyleHandle(DwgHandleRef styleHandle) { this.styleHandle = styleHandle; }
    public void setDataFlags(double dataFlags) { this.dataFlags = dataFlags; }
    public void setElevation(double elevation) { this.elevation = elevation; }
}
