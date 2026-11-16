package io.dwg.render.model.entity;

/** VERTEX of an old-style POLYLINE. */
public class PolylineVertex extends Entity {
    /** a spline frame control point : not part of the drawn curve */
    public static final int FLAG_SPLINE_FRAME = 16;

    public double[] point;
    public double bulge;
    public int flags;
}
