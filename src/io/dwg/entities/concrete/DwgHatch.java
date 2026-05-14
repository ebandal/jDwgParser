package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgObjectType;

/**
 * HATCH 엔티티 (타입 0x4C)
 * 닫힌 영역을 패턴이나 색상으로 채우기
 */
public class DwgHatch extends AbstractDwgEntity {
    private String patternName;
    private int hatchStyle;  // 0=normal, 1=outer, 2=ignore
    private double scale;
    private double angle;
    private int numBoundaryPaths;  // 경계 경로 개수

    @Override
    public DwgObjectType objectType() { return DwgObjectType.HATCH; }

    public String patternName() { return patternName; }
    public int hatchStyle() { return hatchStyle; }
    public double scale() { return scale; }
    public double angle() { return angle; }
    public int numBoundaryPaths() { return numBoundaryPaths; }

    public void setPatternName(String patternName) { this.patternName = patternName; }
    public void setHatchStyle(int hatchStyle) { this.hatchStyle = hatchStyle; }
    public void setScale(double scale) { this.scale = scale; }
    public void setAngle(double angle) { this.angle = angle; }
    public void setNumBoundaryPaths(int numBoundaryPaths) { this.numBoundaryPaths = numBoundaryPaths; }
}
