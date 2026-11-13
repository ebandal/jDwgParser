package io.dwg.render.model.entity;

/** SOLID and TRACE : filled quadrilateral, corners in the order 1, 2, 4, 3 around the outline. */
public class Solid extends Entity {
    public double[][] corners = new double[4][];    // OCS
    public double elevation;
    public double thickness;
    public double[] extrusion;
}
