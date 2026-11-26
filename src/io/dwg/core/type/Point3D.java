package io.dwg.core.type;

/**
 * 3D 좌표 (3BD, 3RD에 대응)
 */
public final class Point3D {
    private final double x;
    private final double y;
    private final double z;

    public Point3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** Recently made points, by hash; a slot holds the last point that fell in it. */
    private static final Point3D[] RECENT = new Point3D[4096];

    /**
     * A point with these coordinates: the same instance as a point made shortly before with
     * equal coordinates (the end of one line is often the start of the next), or a new one. The
     * reader makes its points with this, so that drawings keep fewer of them. Equal as
     * {@link #equals} is (bit for bit).
     */
    public static Point3D of(double x, double y, double z) {
        long bx = Double.doubleToLongBits(x), by = Double.doubleToLongBits(y), bz = Double.doubleToLongBits(z);
        long h = bx * 31 + by;
        h = h * 31 + bz;
        int slot = (int) (h ^ (h >>> 32)) & (RECENT.length - 1);
        Point3D p = RECENT[slot];
        if (p != null && Double.doubleToLongBits(p.x) == bx && Double.doubleToLongBits(p.y) == by
                && Double.doubleToLongBits(p.z) == bz) {
            return p;
        }
        p = new Point3D(x, y, z);
        RECENT[slot] = p;                       // a race only costs sharing: the point is immutable
        return p;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Point3D)) return false;
        Point3D other = (Point3D) o;
        return Double.compare(x, other.x) == 0
                && Double.compare(y, other.y) == 0
                && Double.compare(z, other.z) == 0;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(x, y, z);
    }

    
    /**
     * 이동 후 새 Point3D 반환
     */
    public Point3D translate(double dx, double dy, double dz) {
        return new Point3D(x + dx, y + dy, z + dz);
    }

    /**
     * Z 좌표 버리고 Point2D 반환
     */
    public Point2D toPoint2D() {
        return new Point2D(x, y);
    }

    /**
     * (0, 0, 0) 상수
     */
    public static final Point3D ORIGIN = new Point3D(0.0, 0.0, 0.0);

    /**
     * Z축 단위벡터 (0, 0, 1)
     */
    public static final Point3D Z_AXIS = new Point3D(0.0, 0.0, 1.0);

    @Override
    public String toString() {
        return String.format("Point3D[%.2f, %.2f, %.2f]", x, y, z);
    }
}
