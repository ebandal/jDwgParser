package structure.entities;

import structure.entities.Point3D;
import structure.entities.AbstractDwgEntity;
import structure.entities.DwgObjectType;

/**
 * TOLERANCE 엔티티 (타입 0x2E)
 * 공차 기하공차 표시 (GD&T)
 */
public class DwgTolerance extends AbstractDwgEntity {
    private Point3D insertionPoint;     // 삽입점
    private String dimensionStyleName;  // 크기 지정 스타일명
    private String toleranceText;       // 공차 텍스트
    private double[] direction;         // 방향 벡터

    @Override
    public DwgObjectType objectType() { return DwgObjectType.TOLERANCE; }

    public Point3D insertionPoint() { return insertionPoint; }
    public String dimensionStyleName() { return dimensionStyleName; }
    public String toleranceText() { return toleranceText; }
    public double[] direction() { return direction; }

    public void setInsertionPoint(Point3D insertionPoint) { this.insertionPoint = insertionPoint; }
    public void setDimensionStyleName(String dimensionStyleName) { this.dimensionStyleName = dimensionStyleName; }
    public void setToleranceText(String toleranceText) { this.toleranceText = toleranceText; }
    public void setDirection(double[] direction) { this.direction = direction; }
}
