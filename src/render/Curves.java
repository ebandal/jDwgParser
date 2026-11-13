package render;

import java.util.ArrayList;
import java.util.List;

/** Curves flattened to points { x, y }. */
final class Curves {
    static final int SEGMENTS_PER_CIRCLE = 96;
    private static final int SEGMENTS_PER_SPAN = 12;

    private Curves() {
    }

    /** Circular arc from angle start over sweep (radians, positive = counter-clockwise). */
    static List<double[]> arc(double cx, double cy, double r, double start, double sweep) {
        return ellipse(cx, cy, r, 0.0, 0.0, r, start, sweep);
    }

    /** point(t) = center + major * cos(t) + minor * sin(t), for t from start over sweep. */
    static List<double[]> ellipse(double cx, double cy, double majorX, double majorY,
                                  double minorX, double minorY, double start, double sweep) {
        int n = Math.max(2, (int) Math.ceil(Math.abs(sweep) / (2.0 * Math.PI) * SEGMENTS_PER_CIRCLE));
        List<double[]> points = new ArrayList<>(n + 1);
        for (int i = 0; i <= n; i++) {
            double t = start + sweep * i / n;
            double c = Math.cos(t);
            double s = Math.sin(t);
            points.add(new double[] { cx + majorX * c + minorX * s, cy + majorY * c + minorY * s });
        }
        return points;
    }

    /** Angle from a to b going counter-clockwise, in (0, 2 pi]. */
    static double ccwSweep(double a, double b) {
        double s = (b - a) % (2.0 * Math.PI);
        if (s <= 1e-12) {
            s += 2.0 * Math.PI;
        }
        return s;
    }

    /** Polyline segment a -> b with bulge = tan(included angle / 4), positive = counter-clockwise. Excludes a. */
    static List<double[]> bulge(double ax, double ay, double bx, double by, double bulge) {
        double dx = bx - ax;
        double dy = by - ay;
        double chord = Math.hypot(dx, dy);
        double sweep = 4.0 * Math.atan(bulge);
        double h = chord / 2.0 / Math.tan(sweep / 2.0);     // signed distance chord midpoint -> center
        double cx = (ax + bx) / 2.0 - dy / chord * h;
        double cy = (ay + by) / 2.0 + dx / chord * h;
        double r = Math.hypot(ax - cx, ay - cy);
        List<double[]> points = arc(cx, cy, r, Math.atan2(ay - cy, ax - cx), sweep);
        points.remove(0);
        return points;
    }

    /**
     * B-spline / NURBS curve by de Boor's algorithm.
     * @param ctrl    control points { x, y, ... }
     * @param weights per control point, or null / empty for a non-rational curve
     * @return the curve; the control polygon if the knot vector does not fit
     */
    static List<double[]> nurbs(int degree, double[] knots, double[][] ctrl, double[] weights) {
        int n = ctrl.length;
        int p = degree;
        List<double[]> points = new ArrayList<>();
        if (p < 1 || n <= p || knots == null || knots.length != n + p + 1) {
            for (double[] c : ctrl) {
                points.add(new double[] { c[0], c[1] });
            }
            return points;
        }
        boolean rational = weights != null && weights.length == n;
        double t0 = knots[p];
        double t1 = knots[n];
        int samples = (n - p) * SEGMENTS_PER_SPAN;
        double[][] d = new double[p + 1][3];
        for (int s = 0; s <= samples; s++) {
            double t = t0 + (t1 - t0) * s / samples;
            int k = p;
            while (k < n - 1 && knots[k + 1] <= t) {
                k++;
            }
            for (int j = 0; j <= p; j++) {
                double[] c = ctrl[j + k - p];
                double w = rational ? weights[j + k - p] : 1.0;
                d[j][0] = c[0] * w;
                d[j][1] = c[1] * w;
                d[j][2] = w;
            }
            for (int r = 1; r <= p; r++) {
                for (int j = p; j >= r; j--) {
                    int i = j + k - p;
                    double den = knots[i + p - r + 1] - knots[i];
                    double a = den == 0.0 ? 0.0 : (t - knots[i]) / den;
                    for (int m = 0; m < 3; m++) {
                        d[j][m] = (1.0 - a) * d[j - 1][m] + a * d[j][m];
                    }
                }
            }
            double w = d[p][2] == 0.0 ? 1.0 : d[p][2];
            points.add(new double[] { d[p][0] / w, d[p][1] / w });
        }
        return points;
    }

    /** Smooth curve through the points (Catmull-Rom); an approximation of a spline defined by fit points. */
    static List<double[]> through(double[][] fit) {
        List<double[]> points = new ArrayList<>();
        int n = fit.length;
        if (n == 0) {
            return points;
        }
        points.add(new double[] { fit[0][0], fit[0][1] });
        for (int i = 0; i < n - 1; i++) {
            double[] p0 = fit[Math.max(i - 1, 0)];
            double[] p1 = fit[i];
            double[] p2 = fit[i + 1];
            double[] p3 = fit[Math.min(i + 2, n - 1)];
            for (int s = 1; s <= SEGMENTS_PER_SPAN; s++) {
                double t = (double) s / SEGMENTS_PER_SPAN;
                double[] q = new double[2];
                for (int m = 0; m < 2; m++) {
                    q[m] = 0.5 * (2.0 * p1[m] + (p2[m] - p0[m]) * t
                            + (2.0 * p0[m] - 5.0 * p1[m] + 4.0 * p2[m] - p3[m]) * t * t
                            + (3.0 * p1[m] - p0[m] - 3.0 * p2[m] + p3[m]) * t * t * t);
                }
                points.add(q);
            }
        }
        return points;
    }
}
