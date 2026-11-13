package io.dwg.render.model.entity;

/** 3DFACE */
public class Face3d extends Entity {
    public double[][] corners = new double[4][3];   // WCS
    public int invisibleEdges;                      // bit i : edge i is invisible
}
