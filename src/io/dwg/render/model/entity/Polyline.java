package io.dwg.render.model.entity;

import java.util.ArrayList;
import java.util.List;

/** Old-style POLYLINE (2D or 3D) with its VERTEX entities. */
public class Polyline extends Entity {
    public boolean threeD;                      // false : 2D, the points are in OCS at the elevation
    public boolean closed;
    public double elevation;
    public double[] extrusion = { 0.0, 0.0, 1.0 };
    public final List<double[]> points = new ArrayList<>();     // { x, y, z }
    public final List<Double> bulges = new ArrayList<>();       // per point, 2D only
}
