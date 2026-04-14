package io.dwg.entities.concrete;

import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

import java.util.ArrayList;
import java.util.List;

/**
 * SPLINE 엔티티 (타입 0x24)
 * 제어점을 기반으로 한 B-spline 곡선
 */
public class DwgSpline extends AbstractDwgEntity {
    private int degree;  // B-spline 차수 (1-3)
    private List<Point3D> controlPoints = new ArrayList<>();
    private List<Point3D> fitPoints = new ArrayList<>();
    private double[] extrusion = {0.0, 0.0, 1.0};
    private int flags;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.SPLINE; }

    public int degree() { return degree; }
    public List<Point3D> controlPoints() { return controlPoints; }
    public List<Point3D> fitPoints() { return fitPoints; }
    public double[] extrusion() { return extrusion; }
    public int flags() { return flags; }

    public void setDegree(int degree) { this.degree = degree; }
    public void setControlPoints(List<Point3D> controlPoints) { this.controlPoints = controlPoints; }
    public void setFitPoints(List<Point3D> fitPoints) { this.fitPoints = fitPoints; }
    public void setExtrusion(double[] extrusion) { this.extrusion = extrusion; }
    public void setFlags(int flags) { this.flags = flags; }

    public void addControlPoint(Point3D point) {
        this.controlPoints.add(point);
    }

    public void addFitPoint(Point3D point) {
        this.fitPoints.add(point);
    }
}
