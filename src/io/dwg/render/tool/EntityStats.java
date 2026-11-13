package io.dwg.render.tool;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import io.dwg.render.r2000.DwgParseException;
import io.dwg.render.r2000.EntityDecoderR2000;
import io.dwg.render.r2000.ObjectScannerR2000;
import io.dwg.render.model.DwgObject;
import io.dwg.render.model.entity.Arc;
import io.dwg.render.model.entity.Circle;
import io.dwg.render.model.entity.Entity;
import io.dwg.render.model.entity.Insert;
import io.dwg.render.model.entity.Line;
import io.dwg.render.model.entity.LwPolyline;

/**
 * Decodes LINE, CIRCLE, ARC and LWPOLYLINE of R2000 (AC1015) files and reports
 * how many decoded cleanly and the extents of the model space geometry.
 * Usage: EntityStats [file or directory] [number of sample entities to print]
 */
public class EntityStats {

    public static void main(String[] args) {
        File target = new File(args.length > 0 ? args[0] : "samples/2000");
        int samples = args.length > 1 ? Integer.parseInt(args[1]) : 0;
        File[] files = target.isDirectory()
                ? target.listFiles((d, n) -> n.toLowerCase().endsWith(".dwg"))
                : new File[] { target };
        if (files == null || files.length == 0) {
            System.out.println("no .dwg file in " + target);
            return;
        }
        Arrays.sort(files);

        for (File f : files) {
            ObjectScannerR2000 scanner = new ObjectScannerR2000();
            try {
                scanner.scan(f);
            } catch (Exception e) {
                System.out.println("== " + f.getName() + " : FAILED (" + e + ")");
                continue;
            }

            // per type : { decoded, failed, in model space }
            Map<String, int[]> counts = new TreeMap<>();
            double[] ext = { Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };
            int badNumbers = 0;
            int printed = 0;

            for (DwgObject o : scanner.objects) {
                if (!EntityDecoderR2000.supports(o.type)) {
                    continue;
                }
                int[] c = counts.computeIfAbsent(o.typeName, k -> new int[3]);
                Entity e;
                try {
                    e = EntityDecoderR2000.decode(scanner.file, o, scanner.charset);
                } catch (DwgParseException ex) {
                    c[1]++;
                    continue;
                }
                c[0]++;
                if (!addExtents(e, e.inModelSpace() ? ext : new double[4])) {
                    badNumbers++;
                }
                if (e.inModelSpace()) {
                    c[2]++;
                    if (printed < samples) {
                        System.out.println("   " + describe(e));
                        printed++;
                    }
                }
            }

            System.out.println("== " + f.getName());
            counts.forEach((name, c) -> System.out.printf(
                    "   %-18s decoded=%6d  failed=%4d  model space=%6d%n", name, c[0], c[1], c[2]));
            System.out.println("   non-finite coordinates=" + badNumbers);
            if (ext[0] <= ext[2]) {
                System.out.printf("   model space extents: (%.3f, %.3f) - (%.3f, %.3f)%n", ext[0], ext[1], ext[2], ext[3]);
            } else {
                System.out.println("   model space extents: (none)");
            }
        }
    }

    /** Extends ext = { minX, minY, maxX, maxY }; returns false if a coordinate is not finite. */
    private static boolean addExtents(Entity e, double[] ext) {
        if (e instanceof Line) {
            Line l = (Line) e;
            return add(ext, l.start[0], l.start[1]) & add(ext, l.end[0], l.end[1]);
        } else if (e instanceof Circle) {
            // covers Arc too : the full circle is used as a bound
            Circle c = (Circle) e;
            if (Double.isNaN(c.radius) || Double.isInfinite(c.radius)) {
                return false;
            }
            return add(ext, c.center[0] - c.radius, c.center[1] - c.radius)
                 & add(ext, c.center[0] + c.radius, c.center[1] + c.radius);
        } else if (e instanceof LwPolyline) {
            boolean ok = true;
            for (double[] p : ((LwPolyline) e).points) {
                ok &= add(ext, p[0], p[1]);
            }
            return ok;
        } else if (e instanceof Insert) {
            double[] p = ((Insert) e).insertion;
            return add(ext, p[0], p[1]);
        }
        return true;
    }

    private static boolean add(double[] ext, double x, double y) {
        if (Double.isNaN(x) || Double.isInfinite(x) || Double.isNaN(y) || Double.isInfinite(y)) {
            return false;
        }
        ext[0] = Math.min(ext[0], x);
        ext[1] = Math.min(ext[1], y);
        ext[2] = Math.max(ext[2], x);
        ext[3] = Math.max(ext[3], y);
        return true;
    }

    private static String describe(Entity e) {
        String head = String.format("%X %s color=%d ", e.object.handle, e.object.typeName, e.color);
        if (e instanceof Line) {
            Line l = (Line) e;
            return head + String.format("(%.4f, %.4f, %.4f) -> (%.4f, %.4f, %.4f)",
                    l.start[0], l.start[1], l.start[2], l.end[0], l.end[1], l.end[2]);
        } else if (e instanceof Arc) {
            Arc a = (Arc) e;
            return head + String.format("center=(%.4f, %.4f, %.4f) r=%.4f angle=%.4f..%.4f deg",
                    a.center[0], a.center[1], a.center[2], a.radius,
                    Math.toDegrees(a.startAngle), Math.toDegrees(a.endAngle));
        } else if (e instanceof Circle) {
            Circle c = (Circle) e;
            return head + String.format("center=(%.4f, %.4f, %.4f) r=%.4f", c.center[0], c.center[1], c.center[2], c.radius);
        } else if (e instanceof Insert) {
            Insert ins = (Insert) e;
            return head + String.format("at=(%.4f, %.4f, %.4f) scale=(%.4f, %.4f, %.4f) rotation=%.4f deg block=%X layer=%X",
                    ins.insertion[0], ins.insertion[1], ins.insertion[2], ins.scale[0], ins.scale[1], ins.scale[2],
                    Math.toDegrees(ins.rotation), ins.blockHandle, ins.layerHandle);
        } else {
            LwPolyline p = (LwPolyline) e;
            StringBuilder sb = new StringBuilder(head);
            sb.append("points=").append(p.points.length).append(p.isClosed() ? " closed" : "")
              .append(" bulges=").append(p.bulges.length);
            for (int i = 0; i < Math.min(3, p.points.length); i++) {
                sb.append(String.format(" (%.4f, %.4f)", p.points[i][0], p.points[i][1]));
            }
            return sb.toString();
        }
    }
}
