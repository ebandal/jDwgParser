package io.dwg.render.model.entity;

public class Spline extends Entity {
    public int degree;
    public boolean rational;
    public boolean closed;
    public boolean periodic;
    public double[] knots = new double[0];
    public double[][] controlPoints = new double[0][];  // WCS
    public double[] weights = new double[0];             // per control point, or empty
    public double[][] fitPoints = new double[0][];      // WCS, when the spline is defined by fit points only
}
