package io.dwg.core.type;

/**
 * 2D 좌표 (2BD, 2RD에 대응)
 */
public final class Point2D {
    private final double x;
    private final double y;

    public Point2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Point2D)) return false;
        Point2D other = (Point2D) o;
        return Double.compare(x, other.x) == 0
                && Double.compare(y, other.y) == 0;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(x, y);
    }

    
    /**
     * 이동 후 새 Point2D 반환
     */
    public Point2D translate(double dx, double dy) {
        return new Point2D(x + dx, y + dy);
    }

    /**
     * 두 점 거리 계산
     */
    public double distanceTo(Point2D other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * (0, 0) 상수
     */
    public static final Point2D ORIGIN = new Point2D(0.0, 0.0);

    @Override
    public String toString() {
        return String.format("Point2D[%.2f, %.2f]", x, y);
    }
}
