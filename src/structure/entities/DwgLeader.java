package structure.entities;

import structure.entities.Point3D;
import structure.entities.AbstractDwgEntity;
import structure.entities.DwgObjectType;

import java.util.ArrayList;
import java.util.List;

/**
 * LEADER 엔티티 (타입 0x2D)
 * 주석을 가리키는 선 (화살표 포함)
 */
public class DwgLeader extends AbstractDwgEntity {
    private List<Point3D> points = new ArrayList<>();
    private String styleName;
    private int arrow;  // 화살표 모양
    private double[] extrusion = {0.0, 0.0, 1.0};

    @Override
    public DwgObjectType objectType() { return DwgObjectType.LEADER; }

    public List<Point3D> points() { return points; }
    public String styleName() { return styleName; }
    public int arrow() { return arrow; }
    public double[] extrusion() { return extrusion; }

    public void setPoints(List<Point3D> points) { this.points = points; }
    public void addPoint(Point3D point) { this.points.add(point); }
    public void setStyleName(String styleName) { this.styleName = styleName; }
    public void setArrow(int arrow) { this.arrow = arrow; }
    public void setExtrusion(double[] extrusion) { this.extrusion = extrusion; }
}
