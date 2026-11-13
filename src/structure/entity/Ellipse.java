package structure.entity;

/** All values are in WCS. point(t) = center + majorAxis * cos(t) + minorAxis * sin(t) */
public class Ellipse extends Entity {
    public double[] center;
    public double[] majorAxis;                  // end point of the major axis, relative to the center
    public double[] extrusion;
    public double ratio;                        // minor / major
    public double startParam;                   // radians
    public double endParam;
}
