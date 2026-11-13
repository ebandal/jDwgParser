package io.dwg.render.model.entity;

public class Leader extends Entity {
    public int pathType;                        // 0 straight segments, 1 spline
    public double[][] points;                   // WCS
    public boolean arrowhead;
}
