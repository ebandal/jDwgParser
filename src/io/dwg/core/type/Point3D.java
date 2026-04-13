package io.dwg.core.type;

/**
 * 3D 좌표 (3BD, 3RD에 대응)
 * Java 16+ record
 */
public record Point3D(double x, double y, double z) {
    
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
