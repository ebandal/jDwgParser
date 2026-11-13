package io.dwg.render.r2000;

import java.nio.charset.Charset;

import io.dwg.render.model.DwgObject;
import io.dwg.render.model.entity.Arc;
import io.dwg.render.model.entity.Circle;
import io.dwg.render.model.entity.Dimension;
import io.dwg.render.model.entity.Ellipse;
import io.dwg.render.model.entity.Entity;
import io.dwg.render.model.entity.Face3d;
import io.dwg.render.model.entity.Hatch;
import io.dwg.render.model.entity.Insert;
import io.dwg.render.model.entity.Leader;
import io.dwg.render.model.entity.Line;
import io.dwg.render.model.entity.LwPolyline;
import io.dwg.render.model.entity.MText;
import io.dwg.render.model.entity.Point;
import io.dwg.render.model.entity.PolyfaceMesh;
import io.dwg.render.model.entity.PolyfaceVertex;
import io.dwg.render.model.entity.Solid;
import io.dwg.render.model.entity.Spline;
import io.dwg.render.model.entity.Text;

/**
 * Decodes the body of R2000 (AC1015) entities found by {@link ObjectScannerR2000}.
 * See {@link #supports(int)} for the supported types.
 */
public class EntityDecoderR2000 {
    public static final int TYPE_TEXT = 0x01;
    public static final int TYPE_ATTRIB = 0x02;
    public static final int TYPE_SEQEND = 0x06;
    public static final int TYPE_INSERT = 0x07;
    public static final int TYPE_VERTEX_PFACE = 0x0D;
    public static final int TYPE_VERTEX_PFACE_FACE = 0x0E;
    public static final int TYPE_ARC = 0x11;
    public static final int TYPE_CIRCLE = 0x12;
    public static final int TYPE_LINE = 0x13;
    public static final int TYPE_DIMENSION_FIRST = 0x14;
    public static final int TYPE_DIMENSION_LAST = 0x1A;
    public static final int TYPE_POINT = 0x1B;
    public static final int TYPE_3DFACE = 0x1C;
    public static final int TYPE_POLYLINE_PFACE = 0x1D;
    public static final int TYPE_SOLID = 0x1F;
    public static final int TYPE_TRACE = 0x20;
    public static final int TYPE_ELLIPSE = 0x23;
    public static final int TYPE_SPLINE = 0x24;
    public static final int TYPE_MTEXT = 0x2C;
    public static final int TYPE_LEADER = 0x2D;
    public static final int TYPE_LWPOLYLINE = 0x4D;
    public static final int TYPE_HATCH = 0x4E;

    private static final int MAX_COUNT = 10_000_000;

    public static boolean supports(int type) {
        switch (type) {
        case TYPE_TEXT: case TYPE_ATTRIB: case TYPE_INSERT:
        case TYPE_VERTEX_PFACE: case TYPE_VERTEX_PFACE_FACE: case TYPE_POLYLINE_PFACE:
        case TYPE_ARC: case TYPE_CIRCLE: case TYPE_LINE: case TYPE_POINT: case TYPE_3DFACE:
        case TYPE_SOLID: case TYPE_TRACE: case TYPE_ELLIPSE: case TYPE_SPLINE:
        case TYPE_MTEXT: case TYPE_LEADER: case TYPE_LWPOLYLINE: case TYPE_HATCH:
            return true;
        default:
            return type >= TYPE_DIMENSION_FIRST && type <= TYPE_DIMENSION_LAST;
        }
    }

    public static Entity decode(byte[] file, DwgObject obj) throws DwgParseException {
        return decode(file, obj, null);
    }

    /**
     * @param charset character set of the texts, null for ISO-8859-1
     * @return the decoded entity, or null if the type is not supported
     * @throws DwgParseException if the data or the handles do not end exactly where the object header says they do
     */
    public static Entity decode(byte[] file, DwgObject obj, Charset charset) throws DwgParseException {
        if (!supports(obj.type)) {
            return null;
        }
        try {
            BitReader r = new BitReader(file, 0).charset(charset);
            r.seekBit(obj.bodyBitPos);
            long handleStart = (long) obj.dataOffset * 8 + obj.bitSize;

            Entity e = create(obj.type);
            e.object = obj;
            readCommon(r, e);

            int extraHandles = 0;
            if (e instanceof Dimension) {
                // only the block that holds the picture of the dimension is used; it is in the handle stream
                r.seekBit(handleStart);
            } else {
                extraHandles = readBody(r, e);
                // the entity data must end where the handle stream starts
                if (r.bitPosition() != handleStart) {
                    throw new DwgParseException();
                }
            }

            readCommonHandles(r, e);
            if (e instanceof Insert) {
                Insert ins = (Insert) e;
                ins.blockHandle = r.readHandleRef(obj.handle);
                extraHandles = ins.hasAttribs ? 3 : 0;      // first attrib, last attrib, seqend
            } else if (e instanceof Dimension) {
                r.readHandle();                             // dimension style
                ((Dimension) e).blockHandle = r.readHandleRef(obj.handle);
            }
            for (int i = 0; i < extraHandles; i++) {
                r.readHandle();
            }

            // the handles must end at the end of the object, up to padding bits
            long padding = (long) (obj.dataOffset + obj.size) * 8 - r.bitPosition();
            if (padding < 0 || padding > 7) {
                throw new DwgParseException();
            }
            return e;
        } catch (RuntimeException ex) {
            throw new DwgParseException();
        }
    }

    private static Entity create(int type) {
        switch (type) {
        case TYPE_TEXT: case TYPE_ATTRIB:   return new Text();
        case TYPE_INSERT:                   return new Insert();
        case TYPE_VERTEX_PFACE:
        case TYPE_VERTEX_PFACE_FACE:        return new PolyfaceVertex();
        case TYPE_POLYLINE_PFACE:           return new PolyfaceMesh();
        case TYPE_ARC:                      return new Arc();
        case TYPE_CIRCLE:                   return new Circle();
        case TYPE_LINE:                     return new Line();
        case TYPE_POINT:                    return new Point();
        case TYPE_3DFACE:                   return new Face3d();
        case TYPE_SOLID: case TYPE_TRACE:   return new Solid();
        case TYPE_ELLIPSE:                  return new Ellipse();
        case TYPE_SPLINE:                   return new Spline();
        case TYPE_MTEXT:                    return new MText();
        case TYPE_LEADER:                   return new Leader();
        case TYPE_LWPOLYLINE:               return new LwPolyline();
        case TYPE_HATCH:                    return new Hatch();
        default:                            return new Dimension();
        }
    }

    /** @return the number of handles that follow the common entity handles */
    private static int readBody(BitReader r, Entity e) {
        switch (e.object.type) {
        case TYPE_TEXT:                 readText(r, (Text) e);                          return 1;   // style
        case TYPE_ATTRIB:               readAttrib(r, (Text) e);                        return 1;   // style
        case TYPE_INSERT:               readInsert(r, (Insert) e);                      return 0;   // read by the caller
        case TYPE_VERTEX_PFACE:         readPolyfacePoint(r, (PolyfaceVertex) e);       return 0;
        case TYPE_VERTEX_PFACE_FACE:    readPolyfaceFace(r, (PolyfaceVertex) e);        return 0;
        case TYPE_POLYLINE_PFACE:       readPolyfaceMesh(r, (PolyfaceMesh) e);          return 3;   // first, last vertex, seqend
        case TYPE_ARC:                  readArc(r, (Arc) e);                            return 0;
        case TYPE_CIRCLE:               readCircle(r, (Circle) e);                      return 0;
        case TYPE_LINE:                 readLine(r, (Line) e);                          return 0;
        case TYPE_POINT:                readPoint(r, (Point) e);                        return 0;
        case TYPE_3DFACE:               readFace3d(r, (Face3d) e);                      return 0;
        case TYPE_SOLID:
        case TYPE_TRACE:                readSolid(r, (Solid) e);                        return 0;
        case TYPE_ELLIPSE:              readEllipse(r, (Ellipse) e);                    return 0;
        case TYPE_SPLINE:               readSpline(r, (Spline) e);                      return 0;
        case TYPE_MTEXT:                readMText(r, (MText) e);                        return 1;   // style
        case TYPE_LEADER:               readLeader(r, (Leader) e);                      return 2;   // annotation, dimension style
        case TYPE_LWPOLYLINE:           readLwPolyline(r, (LwPolyline) e);              return 0;
        case TYPE_HATCH:                return readHatch(r, (Hatch) e);                             // boundary objects
        default:                        throw new IllegalStateException("unsupported type");
        }
    }

    // extended entity data, graphic (proxy) data and the common entity fields
    private static void readCommon(BitReader r, Entity e) {
        // EED : { BS size, H application handle, size bytes }* until size 0
        while (true) {
            int size = r.readBS();
            if (size == 0) {
                break;
            }
            r.readHandle();
            r.seekBit(r.bitPosition() + 8L * size);
        }
        // graphic image
        if (r.readBit()) {
            int size = r.readRL();
            r.seekBit(r.bitPosition() + 8L * size);
        }
        e.entMode = r.readBB();
        e.numReactors = r.readBL();
        e.noLinks = r.readBit();
        e.color = r.readBS();
        e.ltypeScale = r.readBD();
        e.ltypeFlags = r.readBB();
        e.plotstyleFlags = r.readBB();
        e.invisible = (r.readBS() & 1) != 0;
        e.lineweight = r.readRC();
    }

    // handle stream : [owner] [reactors] xdictionary [previous next] layer [linetype] [plotstyle]
    private static void readCommonHandles(BitReader r, Entity e) {
        long self = e.object.handle;
        if (e.entMode == Entity.MODE_OWNED) {
            e.ownerHandle = r.readHandleRef(self);
        }
        for (int i = 0, n = count(e.numReactors); i < n; i++) {
            r.readHandle();
        }
        r.readHandle();                         // xdictionary
        if (!e.noLinks) {
            r.readHandle();                     // previous entity
            r.readHandle();                     // next entity
        }
        e.layerHandle = r.readHandleRef(self);
        if (e.ltypeFlags == 3) {
            r.readHandle();
        }
        if (e.plotstyleFlags == 3) {
            r.readHandle();
        }
    }

    private static int count(int n) {
        if (n < 0 || n > MAX_COUNT) {
            throw new IllegalStateException("bad count " + n);
        }
        return n;
    }

    private static void readLine(BitReader r, Line l) {
        boolean zAreZero = r.readBit();
        l.start[0] = r.readRD();
        l.end[0] = r.readDD(l.start[0]);
        l.start[1] = r.readRD();
        l.end[1] = r.readDD(l.start[1]);
        if (!zAreZero) {
            l.start[2] = r.readRD();
            l.end[2] = r.readDD(l.start[2]);
        }
        l.thickness = r.readBT();
        l.extrusion = r.readBE();
    }

    private static void readCircle(BitReader r, Circle c) {
        c.center = r.read3BD();
        c.radius = r.readBD();
        c.thickness = r.readBT();
        c.extrusion = r.readBE();
    }

    private static void readArc(BitReader r, Arc a) {
        readCircle(r, a);
        a.startAngle = r.readBD();
        a.endAngle = r.readBD();
    }

    private static void readEllipse(BitReader r, Ellipse e) {
        e.center = r.read3BD();
        e.majorAxis = r.read3BD();
        e.extrusion = r.read3BD();
        e.ratio = r.readBD();
        e.startParam = r.readBD();
        e.endParam = r.readBD();
    }

    private static void readPoint(BitReader r, Point p) {
        p.position = r.read3BD();
        r.readBT();                             // thickness
        r.readBE();                             // extrusion
        r.readBD();                             // x axis angle
    }

    private static void readFace3d(BitReader r, Face3d f) {
        boolean hasNoFlags = r.readBit();
        boolean zIsZero = r.readBit();
        f.corners[0][0] = r.readRD();
        f.corners[0][1] = r.readRD();
        if (!zIsZero) {
            f.corners[0][2] = r.readRD();
        }
        for (int i = 1; i < 4; i++) {
            for (int k = 0; k < 3; k++) {
                f.corners[i][k] = r.readDD(f.corners[i - 1][k]);
            }
        }
        if (!hasNoFlags) {
            f.invisibleEdges = r.readBS();
        }
    }

    private static void readSolid(BitReader r, Solid s) {
        s.thickness = r.readBT();
        s.elevation = r.readBD();
        for (int i = 0; i < 4; i++) {
            s.corners[i] = r.read2RD();
        }
        s.extrusion = r.readBE();
    }

    private static void readText(BitReader r, Text t) {
        int flags = r.readRC();                 // a set bit means the field is absent and has its default value
        if ((flags & 0x01) == 0) {
            t.elevation = r.readRD();
        }
        t.insertion = r.read2RD();
        t.alignment = new double[] { t.insertion[0], t.insertion[1] };
        if ((flags & 0x02) == 0) {
            t.alignment[0] = r.readDD(t.insertion[0]);
            t.alignment[1] = r.readDD(t.insertion[1]);
        }
        t.extrusion = r.readBE();
        t.thickness = r.readBT();
        if ((flags & 0x04) == 0) {
            t.oblique = r.readRD();
        }
        if ((flags & 0x08) == 0) {
            t.rotation = r.readRD();
        }
        t.height = r.readRD();
        if ((flags & 0x10) == 0) {
            t.widthFactor = r.readRD();
        }
        t.text = r.readTV();
        if ((flags & 0x20) == 0) {
            t.generation = r.readBS();
        }
        if ((flags & 0x40) == 0) {
            t.hAlign = r.readBS();
        }
        if ((flags & 0x80) == 0) {
            t.vAlign = r.readBS();
        }
    }

    private static void readAttrib(BitReader r, Text t) {
        readText(r, t);
        t.attribute = true;
        t.tag = r.readTV();
        r.readBS();                             // field length
        t.attributeFlags = r.readRC();
    }

    private static void readMText(BitReader r, MText m) {
        m.insertion = r.read3BD();
        m.extrusion = r.read3BD();
        m.xAxis = r.read3BD();
        m.rectWidth = r.readBD();
        m.height = r.readBD();
        m.attachment = r.readBS();
        r.readBS();                             // drawing direction
        r.readBD();                             // extents height
        r.readBD();                             // extents width
        m.text = r.readTV();
        r.readBS();                             // line spacing style
        r.readBD();                             // line spacing factor
        r.readBit();                            // unknown
    }

    private static void readInsert(BitReader r, Insert ins) {
        ins.insertion = r.read3BD();
        switch (r.readBB()) {
        case 0:
            ins.scale[0] = r.readRD();
            ins.scale[1] = r.readDD(ins.scale[0]);
            ins.scale[2] = r.readDD(ins.scale[0]);
            break;
        case 1:
            ins.scale[1] = r.readDD(1.0);
            ins.scale[2] = r.readDD(1.0);
            break;
        case 2:
            ins.scale[0] = r.readRD();
            ins.scale[1] = ins.scale[0];
            ins.scale[2] = ins.scale[0];
            break;
        default:
            break;                              // 1.0, 1.0, 1.0
        }
        ins.rotation = r.readBD();
        ins.extrusion = r.read3BD();
        ins.hasAttribs = r.readBit();
    }

    private static void readLwPolyline(BitReader r, LwPolyline p) {
        p.flag = r.readBS();
        if ((p.flag & 4) != 0) {
            p.constWidth = r.readBD();
        }
        if ((p.flag & 8) != 0) {
            p.elevation = r.readBD();
        }
        if ((p.flag & 2) != 0) {
            p.thickness = r.readBD();
        }
        if ((p.flag & 1) != 0) {
            p.extrusion = r.read3BD();
        }
        int numPoints = count(r.readBL());
        int numBulges = (p.flag & 16) != 0 ? count(r.readBL()) : 0;
        int numWidths = (p.flag & 32) != 0 ? count(r.readBL()) : 0;
        if (numBulges > numPoints || numWidths > numPoints) {
            throw new IllegalStateException("bad LWPOLYLINE counts");
        }

        p.points = new double[numPoints][];
        for (int i = 0; i < numPoints; i++) {
            if (i == 0) {
                p.points[i] = r.read2RD();
            } else {
                double x = r.readDD(p.points[i - 1][0]);
                double y = r.readDD(p.points[i - 1][1]);
                p.points[i] = new double[] { x, y };
            }
        }
        p.bulges = new double[numBulges];
        for (int i = 0; i < numBulges; i++) {
            p.bulges[i] = r.readBD();
        }
        p.widths = new double[numWidths][];
        for (int i = 0; i < numWidths; i++) {
            p.widths[i] = new double[] { r.readBD(), r.readBD() };
        }
    }

    private static void readSpline(BitReader r, Spline s) {
        int scenario = r.readBL();
        s.degree = r.readBL();
        int numKnots = 0;
        int numControl = 0;
        int numFit = 0;
        boolean weighted = false;
        if (scenario == 2) {
            r.readBD();                         // fit tolerance
            r.read3BD();                        // begin tangent
            r.read3BD();                        // end tangent
            numFit = count(r.readBL());
        } else if (scenario == 1) {
            s.rational = r.readBit();
            s.closed = r.readBit();
            s.periodic = r.readBit();
            r.readBD();                         // knot tolerance
            r.readBD();                         // control point tolerance
            numKnots = count(r.readBL());
            numControl = count(r.readBL());
            weighted = r.readBit();
        } else {
            throw new IllegalStateException("bad SPLINE scenario " + scenario);
        }
        s.knots = new double[numKnots];
        for (int i = 0; i < numKnots; i++) {
            s.knots[i] = r.readBD();
        }
        s.controlPoints = new double[numControl][];
        s.weights = new double[weighted ? numControl : 0];
        for (int i = 0; i < numControl; i++) {
            s.controlPoints[i] = r.read3BD();
            if (weighted) {
                s.weights[i] = r.readBD();
            }
        }
        s.fitPoints = new double[numFit][];
        for (int i = 0; i < numFit; i++) {
            s.fitPoints[i] = r.read3BD();
        }
    }

    private static void readLeader(BitReader r, Leader l) {
        r.readBit();                            // unknown
        r.readBS();                             // annotation type
        l.pathType = r.readBS();
        int numPoints = count(r.readBL());
        l.points = new double[numPoints][];
        for (int i = 0; i < numPoints; i++) {
            l.points[i] = r.read3BD();
        }
        r.read3BD();                            // origin
        r.read3BD();                            // extrusion
        r.read3BD();                            // x direction
        r.read3BD();                            // offset to the block insertion point
        r.read3BD();                            // end point projection
        r.readBD();                             // box height
        r.readBD();                             // box width
        r.readBit();                            // hook line on x direction
        l.arrowhead = r.readBit();
        r.readBS();                             // unknown
        r.readBit();                            // unknown
        r.readBit();                            // unknown
    }

    private static void readPolyfaceMesh(BitReader r, PolyfaceMesh m) {
        m.numVertices = r.readBS();
        m.numFaces = r.readBS();
    }

    private static void readPolyfacePoint(BitReader r, PolyfaceVertex v) {
        r.readRC();                             // flags
        v.point = r.read3BD();
    }

    private static void readPolyfaceFace(BitReader r, PolyfaceVertex v) {
        v.indexes = new int[4];
        for (int i = 0; i < 4; i++) {
            v.indexes[i] = (short) r.readBS();
        }
    }

    /** @return the number of boundary object handles */
    private static int readHatch(BitReader r, Hatch h) {
        h.elevation = r.readBD();
        h.extrusion = r.read3BD();
        h.name = r.readTV();
        h.solidFill = r.readBit();
        r.readBit();                            // associative
        int numLoops = count(r.readBL());
        int boundaryHandles = 0;
        boolean pixelSize = false;

        for (int i = 0; i < numLoops; i++) {
            Hatch.Loop loop = new Hatch.Loop();
            loop.flag = r.readBL();
            pixelSize |= (loop.flag & 4) != 0;
            loop.polyline = (loop.flag & 2) != 0;
            if (loop.polyline) {
                boolean bulges = r.readBit();
                loop.closed = r.readBit();
                int n = count(r.readBL());
                loop.vertices = new double[n][];
                for (int k = 0; k < n; k++) {
                    double[] p = r.read2RD();
                    loop.vertices[k] = new double[] { p[0], p[1], bulges ? r.readBD() : 0.0 };
                }
            } else {
                loop.closed = true;
                int n = count(r.readBL());
                for (int k = 0; k < n; k++) {
                    loop.edges.add(readHatchEdge(r));
                }
            }
            boundaryHandles += count(r.readBL());
            h.loops.add(loop);
        }

        r.readBS();                             // style
        r.readBS();                             // pattern type
        if (!h.solidFill) {
            r.readBD();                         // angle
            r.readBD();                         // scale or spacing
            r.readBit();                        // double hatch
            int numLines = count(r.readBS());
            for (int i = 0; i < numLines; i++) {
                Hatch.PatternLine pl = new Hatch.PatternLine();
                pl.angle = r.readBD();
                pl.x = r.readBD();
                pl.y = r.readBD();
                pl.offsetX = r.readBD();
                pl.offsetY = r.readBD();
                pl.dashes = new double[count(r.readBS())];
                for (int k = 0; k < pl.dashes.length; k++) {
                    pl.dashes[k] = r.readBD();
                }
                h.pattern.add(pl);
            }
        }
        if (pixelSize) {
            r.readBD();
        }
        int numSeeds = count(r.readBL());
        for (int i = 0; i < numSeeds; i++) {
            r.read2RD();
        }
        return boundaryHandles;
    }

    private static Hatch.Edge readHatchEdge(BitReader r) {
        Hatch.Edge e = new Hatch.Edge();
        e.type = r.readRC();
        e.ccw = true;
        switch (e.type) {
        case Hatch.Edge.LINE:
            e.v = new double[] { r.readRD(), r.readRD(), r.readRD(), r.readRD() };
            break;
        case Hatch.Edge.ARC:
            e.v = new double[] { r.readRD(), r.readRD(), r.readBD(), r.readBD(), r.readBD() };
            e.ccw = r.readBit();
            break;
        case Hatch.Edge.ELLIPSE:
            e.v = new double[] { r.readRD(), r.readRD(), r.readRD(), r.readRD(), r.readBD(), r.readBD(), r.readBD() };
            e.ccw = r.readBit();
            break;
        case Hatch.Edge.SPLINE:
            e.degree = r.readBL();
            e.rational = r.readBit();
            r.readBit();                        // periodic
            int numKnots = count(r.readBL());
            int numControl = count(r.readBL());
            e.knots = new double[numKnots];
            for (int i = 0; i < numKnots; i++) {
                e.knots[i] = r.readBD();
            }
            e.controlPoints = new double[numControl][];
            for (int i = 0; i < numControl; i++) {
                double[] p = r.read2RD();
                e.controlPoints[i] = new double[] { p[0], p[1], e.rational ? r.readBD() : 1.0 };
            }
            break;
        default:
            throw new IllegalStateException("bad HATCH edge type " + e.type);
        }
        return e;
    }
}
