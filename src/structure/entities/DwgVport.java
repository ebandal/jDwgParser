package structure.entities;

import structure.entities.Point3D;
import structure.entities.AbstractDwgObject;
import structure.entities.DwgNonEntityObject;
import structure.entities.DwgObjectType;

/**
 * VPORT 테이블 엔트리 (타입 0x38)
 * 뷰포트 설정 (뷰 윈도우 및 기본값)
 */
public class DwgVport extends AbstractDwgObject implements DwgNonEntityObject {
    private String name = "";
    private Point3D viewCenter;
    private Point3D snapBase;
    private double gridSpacingX = 1.0;
    private double gridSpacingY = 1.0;

    @Override
    public DwgObjectType objectType() { return DwgObjectType.VPORT; }

    @Override
    public boolean isEntity() { return false; }

    public String name() { return name; }
    public Point3D viewCenter() { return viewCenter; }
    public Point3D snapBase() { return snapBase; }
    public double gridSpacingX() { return gridSpacingX; }
    public double gridSpacingY() { return gridSpacingY; }

    public void setName(String name) { this.name = name; }
    public void setViewCenter(Point3D center) { this.viewCenter = center; }
    public void setSnapBase(Point3D base) { this.snapBase = base; }
    public void setGridSpacingX(double x) { this.gridSpacingX = x; }
    public void setGridSpacingY(double y) { this.gridSpacingY = y; }
}
