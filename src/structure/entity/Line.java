package structure.entity;

public class Line extends Entity {
    public double[] start = new double[3];      // WCS
    public double[] end = new double[3];        // WCS
    public double thickness;
    public double[] extrusion;
}
