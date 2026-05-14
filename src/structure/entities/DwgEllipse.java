package structure.entities;

import structure.entities.Point3D;
import structure.entities.AbstractDwgEntity;
import structure.entities.DwgObjectType;

/**
 * ELLIPSE 엔티티 (타입 0x23)
 * 타원
 */
public class DwgEllipse extends AbstractDwgEntity {
    private Point3D center;
    private Point3D majorAxisVec;
    private double[] extrusion = {0.0, 0.0, 1.0};
    private double axisRatio;
    private double startParam;
    private double endParam;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.ELLIPSE; }

    public Point3D center() { return center; }
    public Point3D majorAxisVec() { return majorAxisVec; }
    public double[] extrusion() { return extrusion; }
    public double axisRatio() { return axisRatio; }
    public double startParam() { return startParam; }
    public double endParam() { return endParam; }

    public double majorRadius() {
        if (majorAxisVec == null) return 0.0;
        return Math.sqrt(majorAxisVec.x() * majorAxisVec.x() +
                        majorAxisVec.y() * majorAxisVec.y() +
                        majorAxisVec.z() * majorAxisVec.z());
    }

    public double minorRadius() {
        return majorRadius() * axisRatio;
    }

    public void setCenter(Point3D center) { this.center = center; }
    public void setMajorAxisVec(Point3D majorAxisVec) { this.majorAxisVec = majorAxisVec; }
    public void setExtrusion(double[] extrusion) { this.extrusion = extrusion; }
    public void setAxisRatio(double axisRatio) { this.axisRatio = axisRatio; }
    public void setStartParam(double startParam) { this.startParam = startParam; }
    public void setEndParam(double endParam) { this.endParam = endParam; }
}
