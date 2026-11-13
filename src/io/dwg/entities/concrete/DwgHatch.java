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

    private double elevation;
    private double[] extrusion = {0.0, 0.0, 1.0};
    private boolean solidFill;
    private boolean associative;
    private int patternType;
    private boolean doubleHatch;
    private int boundaryHandleCount;
    private final java.util.List<Loop> loops = new java.util.ArrayList<>();
    private final java.util.List<PatternLine> patternLines = new java.util.ArrayList<>();

    public double elevation() { return elevation; }
    public double[] extrusion() { return extrusion; }
    public boolean isSolidFill() { return solidFill; }
    public boolean isAssociative() { return associative; }
    public int patternType() { return patternType; }
    public boolean isDoubleHatch() { return doubleHatch; }
    /** 경계를 이루는 객체의 핸들 수 (핸들 스트림에 그만큼 기록된다) */
    public int boundaryHandleCount() { return boundaryHandleCount; }
    /** 경계 루프. 좌표는 OCS. */
    public java.util.List<Loop> loops() { return loops; }
    /** 패턴을 이루는 평행선 묶음. 솔리드 채움이면 비어 있다. */
    public java.util.List<PatternLine> patternLines() { return patternLines; }

    public void setElevation(double elevation) { this.elevation = elevation; }
    public void setExtrusion(double[] extrusion) { this.extrusion = extrusion; }
    public void setSolidFill(boolean solidFill) { this.solidFill = solidFill; }
    public void setAssociative(boolean associative) { this.associative = associative; }
    public void setPatternType(int patternType) { this.patternType = patternType; }
    public void setDoubleHatch(boolean doubleHatch) { this.doubleHatch = doubleHatch; }
    public void setBoundaryHandleCount(int boundaryHandleCount) { this.boundaryHandleCount = boundaryHandleCount; }

    /** 경계 루프 하나 : 폴리라인이거나 모서리의 나열 */
    public static class Loop {
        public int flags;
        public boolean polyline;
        public boolean closed;
        /** 폴리라인 루프 : { x, y, bulge } */
        public final java.util.List<double[]> vertices = new java.util.ArrayList<>();
        public final java.util.List<Edge> edges = new java.util.ArrayList<>();
    }

    /** 루프의 모서리 */
    public static class Edge {
        public static final int LINE = 1, ARC = 2, ELLIPSE = 3, SPLINE = 4;
        public int type;
        /**
         * LINE : x1 y1 x2 y2 /
         * ARC : cx cy radius startAngle endAngle /
         * ELLIPSE : cx cy majorX majorY ratio startAngle endAngle
         */
        public double[] values;
        public boolean counterClockwise = true;
        public int degree;
        public boolean rational;
        public boolean periodic;
        public double[] knots;
        /** SPLINE : { x, y, weight } */
        public double[][] controlPoints;
    }

    /** 패턴의 평행선 묶음 하나. 값은 이미 축척과 회전이 적용된 상태이다. */
    public static class PatternLine {
        public double angle;            // 라디안
        public double x, y;             // 첫 번째 선 위의 점
        public double offsetX, offsetY; // 한 선에서 다음 선까지
        public double[] dashes;         // > 0 선, < 0 틈, 0 점; 비어 있으면 실선
    }
}
