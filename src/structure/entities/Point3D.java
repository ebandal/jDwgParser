package structure.entities;

public record Point3D(double x, double y, double z) {

    public Point3D translate(double dx, double dy, double dz) {
        return new Point3D(x + dx, y + dy, z + dz);
    }

    public Point2D toPoint2D() {
        return new Point2D(x, y);
    }

    public static final Point3D ORIGIN = new Point3D(0.0, 0.0, 0.0);
    public static final Point3D Z_AXIS = new Point3D(0.0, 0.0, 1.0);

    @Override
    public String toString() {
        return String.format("Point3D[%.2f, %.2f, %.2f]", x, y, z);
    }
}
