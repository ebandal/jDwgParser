package structure.entities;

import structure.entities.Point3D;
import structure.entities.AbstractDwgEntity;
import structure.entities.DwgObjectType;

/**
 * VERTEX_2D 엔티티 (타입 0x0A)
 * 2D 폴리라인의 꼭지점
 */
public class DwgVertex2D extends AbstractDwgEntity {
    private Point3D location;
    private double startWidth;   // 이 점에서의 선 시작 폭
    private double endWidth;     // 이 점에서의 선 끝 폭
    private double bulge;        // 호 불룩함 (0=직선, >0=호)
    private int flags;           // 꼭지점 플래그

    @Override
    public DwgObjectType objectType() { return DwgObjectType.VERTEX_2D; }

    public Point3D location() { return location; }
    public double startWidth() { return startWidth; }
    public double endWidth() { return endWidth; }
    public double bulge() { return bulge; }
    public int flags() { return flags; }

    public void setLocation(Point3D location) { this.location = location; }
    public void setStartWidth(double startWidth) { this.startWidth = startWidth; }
    public void setEndWidth(double endWidth) { this.endWidth = endWidth; }
    public void setBulge(double bulge) { this.bulge = bulge; }
    public void setFlags(int flags) { this.flags = flags; }
}
