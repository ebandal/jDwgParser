package io.dwg.render.model.entity;

public class LwPolyline extends Entity {
    public static final int FLAG_CLOSED = 512;
    public static final int FLAG_PLINEGEN = 256;

    public int flag;
    public double constWidth;
    public double elevation;
    public double thickness;
    public double[] extrusion = { 0.0, 0.0, 1.0 };
    public double[][] points;                   // [n][2], OCS
    public double[] bulges;                     // per vertex, or empty
    public double[][] widths;                   // [n][2] start/end width, or empty

    public boolean isClosed() {
        return (flag & FLAG_CLOSED) != 0;
    }
}
