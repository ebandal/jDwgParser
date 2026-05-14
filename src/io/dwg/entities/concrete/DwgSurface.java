package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * SURFACE 엔티티 - NURBS 곡면 (R2007+)
 */
public class DwgSurface extends AbstractDwgEntity {
    private int type;  // 0=Plane, 1=Cylindrical, 2=Conical, 3=Spherical, 4=Toroidal, 5=Surface of revolution, 6=Tabulated, 7=NURBS
    private int degreeU;
    private int degreeV;
    private int numControlPointsU;
    private int numControlPointsV;
    private int numKnotsU;
    private int numKnotsV;
    private double[][] controlPoints;  // Array of 3D control points
    private double[] knotsU;
    private double[] knotsV;
    private double[] weights;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.SURFACE; }

    public int type() { return type; }
    public int degreeU() { return degreeU; }
    public int degreeV() { return degreeV; }
    public int numControlPointsU() { return numControlPointsU; }
    public int numControlPointsV() { return numControlPointsV; }
    public int numKnotsU() { return numKnotsU; }
    public int numKnotsV() { return numKnotsV; }
    public double[][] controlPoints() { return controlPoints; }
    public double[] knotsU() { return knotsU; }
    public double[] knotsV() { return knotsV; }
    public double[] weights() { return weights; }

    public void setType(int type) { this.type = type; }
    public void setDegreeU(int degreeU) { this.degreeU = degreeU; }
    public void setDegreeV(int degreeV) { this.degreeV = degreeV; }
    public void setNumControlPointsU(int numControlPointsU) { this.numControlPointsU = numControlPointsU; }
    public void setNumControlPointsV(int numControlPointsV) { this.numControlPointsV = numControlPointsV; }
    public void setNumKnotsU(int numKnotsU) { this.numKnotsU = numKnotsU; }
    public void setNumKnotsV(int numKnotsV) { this.numKnotsV = numKnotsV; }
    public void setControlPoints(double[][] controlPoints) { this.controlPoints = controlPoints; }
    public void setKnotsU(double[] knotsU) { this.knotsU = knotsU; }
    public void setKnotsV(double[] knotsV) { this.knotsV = knotsV; }
    public void setWeights(double[] weights) { this.weights = weights; }
}
