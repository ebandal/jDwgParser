package structure.entities;

public record Point2D(double x, double y) {

    public Point2D translate(double dx, double dy) {
        return new Point2D(x + dx, y + dy);
    }

    public double distanceTo(Point2D other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public double getX() { return x; }
    public double getY() { return y; }

    public static final Point2D ORIGIN = new Point2D(0.0, 0.0);

    @Override
    public String toString() {
        return String.format("Point2D[%.2f, %.2f]", x, y);
    }
}
