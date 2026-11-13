package io.dwg.render.tool;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.core.type.Point2D;
import io.dwg.core.type.Point3D;
import io.dwg.entities.concrete.DwgArc;
import io.dwg.entities.concrete.DwgBlockHeader;
import io.dwg.entities.concrete.DwgCircle;
import io.dwg.entities.concrete.DwgEllipse;
import io.dwg.entities.concrete.DwgFace3D;
import io.dwg.entities.concrete.DwgHatch;
import io.dwg.entities.concrete.DwgInsert;
import io.dwg.entities.concrete.DwgLayer;
import io.dwg.entities.concrete.DwgLine;
import io.dwg.entities.concrete.DwgLwPolyline;
import io.dwg.entities.concrete.DwgMText;
import io.dwg.entities.concrete.DwgPoint;
import io.dwg.entities.concrete.DwgSolid;
import io.dwg.entities.concrete.DwgSpline;
import io.dwg.entities.concrete.DwgText;
import io.dwg.render.model.BlockHeader;
import io.dwg.render.model.DwgObject;
import io.dwg.render.model.Layer;
import io.dwg.render.model.entity.Arc;
import io.dwg.render.model.entity.Circle;
import io.dwg.render.model.entity.Ellipse;
import io.dwg.render.model.entity.Entity;
import io.dwg.render.model.entity.Face3d;
import io.dwg.render.model.entity.Hatch;
import io.dwg.render.model.entity.Insert;
import io.dwg.render.model.entity.Line;
import io.dwg.render.model.entity.LwPolyline;
import io.dwg.render.model.entity.MText;
import io.dwg.render.model.entity.Point;
import io.dwg.render.model.entity.Solid;
import io.dwg.render.model.entity.Spline;
import io.dwg.render.model.entity.Text;
import io.dwg.render.r2000.EntityDecoderR2000;
import io.dwg.render.r2000.ObjectScannerR2000;
import io.dwg.render.r2000.TableDecoderR2000;

/**
 * Checks what {@link DwgReader} returns for R2000 (AC1015) files against the reader of io.dwg.render.r2000,
 * object by object. The reference reader accepts an object only if its data ends exactly where the object
 * header says, so its values are taken as correct.
 *
 * For each type it prints : objects in the files, objects DwgReader returned under the same handle as the
 * right class, and objects whose values are equal. For entities "values" include the entity mode, color,
 * visibility and the handles of the layer, the owner and (INSERT, DIMENSION) the block.
 *
 * Usage: ReaderCheck [file or directory]...   (default: samples/2000)
 */
public class ReaderCheck {
    private static final double TOLERANCE = 1e-9;

    /** Outcome of {@link #check(List)}. */
    public static final class Result {
        public int files;
        /** objects in the files, and those DwgReader returned under the same handle */
        public int objects;
        public int objectsFound;
        /** per type : { in the files, found as the right class, values equal } */
        public final Map<String, int[]> types = new TreeMap<>();
        /** the first difference of each type */
        public final List<String> differences = new ArrayList<>();
        /** files that were not compared, with the reason */
        public final List<String> skipped = new ArrayList<>();

        /** true if every object was returned and every compared object is equal */
        public boolean agrees() {
            return skipped.isEmpty() && objects == objectsFound
                    && types.values().stream().allMatch(t -> t[0] == t[1] && t[1] == t[2]);
        }
    }

    public static void main(String[] args) throws Exception {
        Logger.getLogger("").setLevel(Level.OFF);
        Result result = check(collect(args.length > 0 ? args : new String[] { "samples/2000" }));

        result.skipped.forEach(System.out::println);
        System.out.printf("files=%d, objects=%d, returned by DwgReader under the same handle=%d%n%n",
                result.files, result.objects, result.objectsFound);
        System.out.printf("%-20s %9s %12s %13s%n", "type", "in files", "right class", "values equal");
        result.types.forEach((name, t) -> System.out.printf("%-20s %9d %12d %13d%n", name, t[0], t[1], t[2]));
        if (!result.differences.isEmpty()) {
            System.out.println();
            System.out.println("first difference of each type:");
            result.differences.forEach(e -> System.out.println("  " + e));
        }
    }

    /** The .dwg files named by the arguments; a directory stands for the files directly in it. */
    public static List<File> collect(String... paths) {
        List<File> files = new ArrayList<>();
        for (String a : paths) {
            File target = new File(a);
            File[] list = target.isDirectory()
                    ? target.listFiles((d, n) -> n.toLowerCase().endsWith(".dwg")) : new File[] { target };
            if (list != null) {
                Arrays.sort(list);
                files.addAll(Arrays.asList(list));
            }
        }
        return files;
    }

    /** Compares what DwgReader returns for the files with the reference reader. */
    public static Result check(List<File> files) {
        Result result = new Result();
        result.files = files.size();
        for (File f : files) {
            ObjectScannerR2000 scanner = new ObjectScannerR2000();
            try {
                scanner.scan(f);
            } catch (Exception e) {
                result.skipped.add(f.getName() + " : skipped, not readable as R2000 (" + e + ")");
                continue;
            }
            Map<Long, io.dwg.entities.DwgObject> actual = read(f);
            if (actual == null) {
                result.skipped.add(f.getName() + " : DwgReader failed");
                actual = new TreeMap<>();
            }
            for (DwgObject o : scanner.objects) {
                result.objects++;
                io.dwg.entities.DwgObject a = actual.get(o.handle);
                if (a != null) {
                    result.objectsFound++;
                }
                Object expected;
                try {
                    if (o.type == TableDecoderR2000.TYPE_LAYER) {
                        expected = TableDecoderR2000.decodeLayer(scanner.file, o, scanner.charset);
                    } else if (o.type == TableDecoderR2000.TYPE_BLOCK_HEADER) {
                        expected = TableDecoderR2000.decodeBlockHeader(scanner.file, o, scanner.charset);
                    } else {
                        expected = EntityDecoderR2000.decode(scanner.file, o, scanner.charset);
                    }
                } catch (Exception e) {
                    expected = null;
                }
                if (expected == null) {
                    continue;
                }
                int[] t = result.types.computeIfAbsent(o.typeName, k -> new int[3]);
                t[0]++;
                String difference = compare(expected, a);
                if (difference == null && expected instanceof Entity) {
                    difference = compareCommon((Entity) expected, a);
                }
                if (difference == null) {
                    t[1]++;
                    t[2]++;
                } else if (!difference.startsWith("class")) {
                    t[1]++;
                }
                if (difference != null && result.differences.stream().noneMatch(x -> x.startsWith(o.typeName + " "))) {
                    String shown = difference.replaceAll("\\p{Cntrl}", "?");
                    result.differences.add(String.format("%s %s handle %X : %s", o.typeName, f.getName(), o.handle,
                            shown.length() > 160 ? shown.substring(0, 160) + "..." : shown));
                }
            }
        }
        return result;
    }

    // DwgReader prints diagnostics while it reads; they are not wanted here
    private static Map<Long, io.dwg.entities.DwgObject> read(File f) {
        PrintStream out = System.out;
        PrintStream err = System.err;
        PrintStream mute = new PrintStream(new ByteArrayOutputStream());
        System.setOut(mute);
        System.setErr(mute);
        try {
            DwgDocument doc = DwgReader.defaultReader().open(f.toPath());
            return doc.objectMap();
        } catch (Throwable t) {
            return null;
        } finally {
            System.setOut(out);
            System.setErr(err);
        }
    }

    /** @return null if equal, else what differs; "class ..." if the object is missing or of another class */
    private static String compare(Object expected, io.dwg.entities.DwgObject actual) {
        if (actual == null) {
            return "class : no object under this handle";
        }
        try {
            if (expected instanceof Line) {
                Line e = (Line) expected;
                if (!(actual instanceof DwgLine)) return wrongClass(actual);
                DwgLine a = (DwgLine) actual;
                return diff("start", e.start, xyz(a.start()), "end", e.end, xyz(a.end()));
            } else if (expected instanceof Arc) {
                Arc e = (Arc) expected;
                if (!(actual instanceof DwgArc)) return wrongClass(actual);
                DwgArc a = (DwgArc) actual;
                return diff("center", e.center, xyz(a.center()), "radius/angles",
                        new double[] { e.radius, e.startAngle, e.endAngle },
                        new double[] { a.radius(), a.startAngle(), a.endAngle() });
            } else if (expected instanceof Circle) {
                Circle e = (Circle) expected;
                if (!(actual instanceof DwgCircle)) return wrongClass(actual);
                DwgCircle a = (DwgCircle) actual;
                return diff("center", e.center, xyz(a.center()), "radius", new double[] { e.radius }, new double[] { a.radius() });
            } else if (expected instanceof Ellipse) {
                Ellipse e = (Ellipse) expected;
                if (!(actual instanceof DwgEllipse)) return wrongClass(actual);
                DwgEllipse a = (DwgEllipse) actual;
                return diff("center", e.center, xyz(a.center()), "major axis", e.majorAxis, xyz(a.majorAxisVec()),
                        "ratio/params", new double[] { e.ratio, e.startParam, e.endParam },
                        new double[] { a.axisRatio(), a.startParam(), a.endParam() });
            } else if (expected instanceof Point) {
                if (!(actual instanceof DwgPoint)) return wrongClass(actual);
                return diff("position", ((Point) expected).position, xyz(((DwgPoint) actual).position()));
            } else if (expected instanceof Text) {
                Text e = (Text) expected;
                // TEXT and ATTRIB share the fields that are compared
                if (!(actual instanceof DwgText)) {
                    return e.attribute && actual instanceof io.dwg.entities.concrete.DwgAttrib ? null : wrongClass(actual);
                }
                DwgText a = (DwgText) actual;
                if (!e.text.equals(a.value())) return "text '" + e.text + "' != '" + a.value() + "'";
                return diff("insertion", e.insertion, xy(a.insertionPoint()), "height/rotation",
                        new double[] { e.height, e.rotation }, new double[] { a.height(), a.rotationAngle() });
            } else if (expected instanceof MText) {
                MText e = (MText) expected;
                if (!(actual instanceof DwgMText)) return wrongClass(actual);
                DwgMText a = (DwgMText) actual;
                if (!e.text.equals(a.text())) return "text '" + e.text + "' != '" + a.text() + "'";
                return diff("insertion", e.insertion, xyz(a.location()), "height", new double[] { e.height }, new double[] { a.height() });
            } else if (expected instanceof LwPolyline) {
                LwPolyline e = (LwPolyline) expected;
                if (!(actual instanceof DwgLwPolyline)) return wrongClass(actual);
                DwgLwPolyline a = (DwgLwPolyline) actual;
                if (a.vertices() == null || a.vertices().size() != e.points.length) {
                    return "vertex count " + e.points.length + " != " + (a.vertices() == null ? "null" : a.vertices().size());
                }
                for (int i = 0; i < e.points.length; i++) {
                    String d = diff("vertex " + i, e.points[i], xy(a.vertices().get(i)));
                    if (d != null) return d;
                }
                return null;
            } else if (expected instanceof Insert) {
                Insert e = (Insert) expected;
                if (!(actual instanceof DwgInsert)) return wrongClass(actual);
                DwgInsert a = (DwgInsert) actual;
                return diff("insertion", e.insertion, xyz(a.insertionPoint()), "scale", e.scale,
                        new double[] { a.xScale(), a.yScale(), a.zScale() }, "rotation",
                        new double[] { e.rotation }, new double[] { a.rotation() });
            } else if (expected instanceof Solid) {
                Solid e = (Solid) expected;
                // SOLID and TRACE have the same fields
                Point3D[] p;
                if (actual instanceof DwgSolid) {
                    p = ((DwgSolid) actual).points();
                } else if (actual instanceof io.dwg.entities.concrete.DwgTrace) {
                    p = ((io.dwg.entities.concrete.DwgTrace) actual).points();
                } else {
                    return wrongClass(actual);
                }
                if (p == null || p.length < 4) return "points missing";
                for (int i = 0; i < 4; i++) {
                    String d = diff("corner " + i, e.corners[i], new double[] { p[i].x(), p[i].y() });
                    if (d != null) return d;
                }
                return null;
            } else if (expected instanceof Face3d) {
                Face3d e = (Face3d) expected;
                if (!(actual instanceof DwgFace3D)) return wrongClass(actual);
                Point3D[] p = ((DwgFace3D) actual).points();
                if (p == null || p.length < 4) return "points missing";
                for (int i = 0; i < 4; i++) {
                    String d = diff("corner " + i, e.corners[i], xyz(p[i]));
                    if (d != null) return d;
                }
                return null;
            } else if (expected instanceof Spline) {
                Spline e = (Spline) expected;
                if (!(actual instanceof DwgSpline)) return wrongClass(actual);
                DwgSpline a = (DwgSpline) actual;
                List<Point3D> cp = a.controlPoints();
                if (cp == null || cp.size() != e.controlPoints.length) {
                    return "control point count " + e.controlPoints.length + " != " + (cp == null ? "null" : cp.size());
                }
                for (int i = 0; i < e.controlPoints.length; i++) {
                    String d = diff("control point " + i, e.controlPoints[i], xyz(cp.get(i)));
                    if (d != null) return d;
                }
                return null;
            } else if (expected instanceof Hatch) {
                return actual instanceof DwgHatch ? null : wrongClass(actual);
            } else if (expected instanceof Layer) {
                Layer e = (Layer) expected;
                if (!(actual instanceof DwgLayer)) return wrongClass(actual);
                DwgLayer a = (DwgLayer) actual;
                if (!e.name.equals(a.name())) return "name '" + e.name + "' != '" + a.name() + "'";
                int color = a.color() == null ? Integer.MIN_VALUE : a.color().getColorIndex();
                if (color != e.color) return "color " + e.color + " != " + color;
                if (a.isFrozen() != e.frozen) return "frozen " + e.frozen + " != " + a.isFrozen();
                return null;
            } else if (expected instanceof BlockHeader) {
                BlockHeader e = (BlockHeader) expected;
                if (!(actual instanceof DwgBlockHeader)) return wrongClass(actual);
                DwgBlockHeader a = (DwgBlockHeader) actual;
                if (!e.name.equals(a.blockName())) return "name '" + e.name + "' != '" + a.blockName() + "'";
                return diff("base point", e.basePoint, xyz(a.basePoint()));
            } else if (expected instanceof Entity) {
                // DIMENSION, LEADER, polyface meshes : only the class is checked
                String name = actual.getClass().getSimpleName().toUpperCase();
                String type = ((Entity) expected).object.typeName.replace("_", "");
                return name.contains(type.replace("POLYLINEPFACE", "POLYLINEPFACE")) ? null : wrongClass(actual);
            }
            return null;
        } catch (RuntimeException ex) {
            return "exception while comparing : " + ex;
        }
    }

    /** Common entity values and the handles that link an entity to its layer, its owner and its block. */
    private static String compareCommon(Entity e, io.dwg.entities.DwgObject actual) {
        if (!(actual instanceof io.dwg.entities.AbstractDwgEntity)) {
            return null;
        }
        io.dwg.entities.AbstractDwgEntity a = (io.dwg.entities.AbstractDwgEntity) actual;
        if (a.entityMode() != e.entMode) {
            return "entity mode " + e.entMode + " != " + a.entityMode();
        }
        int color = a.color() == null ? Integer.MIN_VALUE : a.color().getColorIndex();
        if (color != e.color) {
            return "color " + e.color + " != " + color;
        }
        if (((a.invisibility() & 1) != 0) != e.invisible) {
            return "invisible " + e.invisible + " != " + a.invisibility();
        }
        String d = handleDiff("layer handle", e.layerHandle, a.layerHandle());
        if (d == null && e.entMode == Entity.MODE_OWNED) {
            d = handleDiff("owner handle", e.ownerHandle, a.ownerHandle());
        }
        if (d == null && e instanceof Insert && actual instanceof DwgInsert) {
            d = handleDiff("block handle", ((Insert) e).blockHandle, ((DwgInsert) actual).blockHeaderHandle());
        }
        if (d == null && e instanceof io.dwg.render.model.entity.Dimension && actual instanceof io.dwg.entities.DwgDimension) {
            d = handleDiff("block handle", ((io.dwg.render.model.entity.Dimension) e).blockHandle,
                    ((io.dwg.entities.DwgDimension) actual).blockHandle());
        }
        return d;
    }

    private static String handleDiff(String name, long expected, io.dwg.core.type.DwgHandleRef actual) {
        if (actual == null) {
            return name + " missing";
        }
        return actual.rawHandle() == expected ? null
                : String.format("%s %X != %X", name, expected, actual.rawHandle());
    }

    private static String wrongClass(io.dwg.entities.DwgObject actual) {
        return "class : returned as " + actual.getClass().getSimpleName();
    }

    private static double[] xyz(Point3D p) {
        return p == null ? null : new double[] { p.x(), p.y(), p.z() };
    }

    private static double[] xy(Point2D p) {
        return p == null ? null : new double[] { p.x(), p.y() };
    }

    /** @param fields name, expected, actual triples */
    private static String diff(Object... fields) {
        for (int i = 0; i < fields.length; i += 3) {
            double[] e = (double[]) fields[i + 1];
            double[] a = (double[]) fields[i + 2];
            if (a == null) {
                return fields[i] + " missing";
            }
            for (int k = 0; k < Math.min(e.length, a.length); k++) {
                if (!(Math.abs(e[k] - a[k]) <= TOLERANCE * Math.max(1.0, Math.abs(e[k])))) {
                    return fields[i] + " " + Arrays.toString(e) + " != " + Arrays.toString(a);
                }
            }
        }
        return null;
    }
}
