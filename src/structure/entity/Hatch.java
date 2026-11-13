package structure.entity;

import java.util.ArrayList;
import java.util.List;

public class Hatch extends Entity {
    public double elevation;
    public double[] extrusion;
    public String name;
    public boolean solidFill;
    public List<Loop> loops = new ArrayList<>();        // OCS
    public List<PatternLine> pattern = new ArrayList<>();

    public static class Loop {
        public int flag;
        public boolean polyline;
        public boolean closed;
        public double[][] vertices;                     // polyline : { x, y, bulge }
        public List<Edge> edges = new ArrayList<>();    // otherwise
    }

    public static class Edge {
        public static final int LINE = 1, ARC = 2, ELLIPSE = 3, SPLINE = 4;
        public int type;
        // LINE : x1 y1 x2 y2
        // ARC : cx cy radius startAngle endAngle
        // ELLIPSE : cx cy majorX majorY ratio startAngle endAngle
        public double[] v;
        public boolean ccw;
        public int degree;
        public boolean rational;
        public double[] knots;
        public double[][] controlPoints;                // { x, y, weight }
    }

    /** One family of parallel lines, already scaled and rotated. */
    public static class PatternLine {
        public double angle;                            // radians
        public double x, y;                             // a point of the first line
        public double offsetX, offsetY;                 // from one line to the next
        public double[] dashes;                         // > 0 dash, < 0 gap, 0 dot; empty = continuous
    }
}
