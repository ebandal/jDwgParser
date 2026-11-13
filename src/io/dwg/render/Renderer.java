package io.dwg.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.dwg.render.model.BlockHeader;
import io.dwg.render.model.Drawing;
import io.dwg.render.model.Layer;
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
import io.dwg.render.model.entity.Solid;
import io.dwg.render.model.entity.Spline;
import io.dwg.render.model.entity.Text;

/**
 * Draws the model space of a drawing as a top view (WCS X/Y plane) into an image.
 * <ul>
 * <li>Curves : LINE, CIRCLE, ARC, ELLIPSE, SPLINE, LWPOLYLINE (bulges are drawn, widths are not), LEADER, POINT</li>
 * <li>Faces : 3DFACE and polyface meshes as edges, SOLID / TRACE filled</li>
 * <li>HATCH : solid fill, or pattern lines clipped to the boundary</li>
 * <li>TEXT, MTEXT and attributes, with a system font</li>
 * <li>INSERT and DIMENSION : the referenced block is expanded</li>
 * </ul>
 * Colors follow the entity, its layer (BYLAYER) or the block reference (BYBLOCK).
 * Entities on frozen or switched-off layers are not drawn. Linetypes are not resolved.
 */
public class Renderer {
    private static final int MAX_BLOCK_DEPTH = 32;
    private static final int COLOR_FOREGROUND = 7;

    // drawing order
    private static final int PASS_FILL = 0;
    private static final int PASS_PATTERN = 1;
    private static final int PASS_STROKE = 2;
    private static final int PASS_TEXT = 3;

    // pattern lines closer than this many pixels are replaced by a translucent fill
    private static final double MIN_PATTERN_SPACING = 3.0;
    private static final int MAX_PATTERN_LINES = 4000;
    private static final int MAX_PATTERN_DASHES = 200_000;
    private static final int DENSE_PATTERN_ALPHA = 90;

    public Color background = Color.BLACK;
    public Color foreground = Color.WHITE;
    /** length of the longer image side in pixels */
    public int imageSize = 2000;
    public int margin = 20;

    /** results of the last call of render() */
    public int drawn;               // primitives drawn, after block expansion
    public int insertsExpanded;
    public int hidden;              // primitives and block references skipped : invisible or on a hidden layer
    public int missingBlocks;       // block references whose block is unknown or empty

    private static class Item {
        int pass;
        Shape shape;                // in WCS
        Color color;
        Hatch hatch;                // PASS_PATTERN : the hatch, its boundary in OCS and OCS -> WCS
        Shape area;
        AffineTransform toWorld;
    }

    private Drawing drawing;
    private List<Item> items;
    private Rectangle2D bounds;

    /** @return the image, or null if there is nothing to draw */
    public BufferedImage render(Drawing d) {
        drawing = d;
        items = new ArrayList<>();
        bounds = null;
        drawn = insertsExpanded = hidden = missingBlocks = 0;

        addEntities(d.modelSpace, new AffineTransform(), null, COLOR_FOREGROUND, new HashSet<>());
        drawn = items.size();
        if (bounds == null) {
            return null;
        }

        double w = Math.max(bounds.getWidth(), 1e-9);
        double h = Math.max(bounds.getHeight(), 1e-9);
        double scale = (imageSize - 2.0 * margin) / Math.max(w, h);
        int imgW = (int) Math.ceil(w * scale) + 2 * margin;
        int imgH = (int) Math.ceil(h * scale) + 2 * margin;

        // world -> image : move the lower-left corner to the origin, scale, flip Y
        AffineTransform view = new AffineTransform();
        view.translate(margin, imgH - margin);
        view.scale(scale, -scale);
        view.translate(-bounds.getMinX(), -bounds.getMinY());

        BufferedImage img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(background);
        g.fillRect(0, 0, imgW, imgH);
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int pass = PASS_FILL; pass <= PASS_TEXT; pass++) {
            for (Item item : items) {
                if (item.pass != pass) {
                    continue;
                }
                g.setColor(item.color);
                if (pass == PASS_PATTERN) {
                    drawPattern(g, view, item);
                } else if (pass == PASS_STROKE) {
                    g.draw(view.createTransformedShape(item.shape));
                } else {
                    g.fill(view.createTransformedShape(item.shape));
                }
            }
        }
        g.dispose();
        return img;
    }

    /**
     * @param transform   block coordinates -> WCS of the model space
     * @param insertLayer layer of the enclosing block reference, null at the top level
     * @param insertColor resolved color index of the enclosing block reference
     * @param blockStack  handles of the blocks being expanded, to stop on circular references
     */
    private void addEntities(List<? extends Entity> entities, AffineTransform transform, Layer insertLayer,
                             int insertColor, Set<Long> blockStack) {
        for (Entity e : entities) {
            // inside a block, entities on layer "0" take the layer of the block reference
            Layer layer = drawing.layers.get(e.layerHandle);
            if (layer != null && layer.isLayerZero() && insertLayer != null) {
                layer = insertLayer;
            }
            boolean invisibleAttribute = e instanceof Text && ((Text) e).attribute && (((Text) e).attributeFlags & 1) != 0;
            if (e.invisible || invisibleAttribute || (layer != null && layer.isHidden())) {
                hidden++;
                continue;
            }
            int color = e.color;
            if (color == Entity.COLOR_BYLAYER) {
                color = layer == null ? COLOR_FOREGROUND : Math.abs(layer.color);
            } else if (color == Entity.COLOR_BYBLOCK) {
                color = insertColor;
            }

            if (e instanceof Insert) {
                Insert ins = (Insert) e;
                BlockHeader block = drawing.blocks.get(ins.blockHandle);
                if (block == null) {
                    missingBlocks++;
                } else {
                    AffineTransform t = new AffineTransform(transform);
                    t.concatenate(insertTransform(ins, block));
                    addBlock(ins.blockHandle, t, layer, color, blockStack);
                }
                // attributes are in the coordinates of the space the block reference is in
                addEntities(ins.attribs, transform, layer, color, blockStack);
            } else if (e instanceof Dimension) {
                // the block of a dimension is drawn in place
                addBlock(((Dimension) e).blockHandle, transform, layer, color, blockStack);
            } else {
                addPrimitive(e, transform, colorOf(color));
            }
        }
    }

    private void addBlock(long blockHandle, AffineTransform transform, Layer layer, int color, Set<Long> blockStack) {
        List<Entity> content = drawing.entitiesOf(blockHandle);
        if (content.isEmpty()) {
            missingBlocks++;
            return;
        }
        if (blockStack.size() >= MAX_BLOCK_DEPTH || !blockStack.add(blockHandle)) {
            return;
        }
        insertsExpanded++;
        addEntities(content, transform, layer, color, blockStack);
        blockStack.remove(blockHandle);
    }

    // block coordinates -> coordinates of the space the block reference is in :
    // OCS(extrusion) * translate(insertion) * rotate * scale * translate(-base point)
    private AffineTransform insertTransform(Insert ins, BlockHeader block) {
        AffineTransform t = new Ocs(ins.extrusion).toAffine(ins.insertion[2]);
        t.translate(ins.insertion[0], ins.insertion[1]);
        t.rotate(ins.rotation);
        t.scale(ins.scale[0], ins.scale[1]);
        t.translate(-block.basePoint[0], -block.basePoint[1]);
        return t;
    }

    private void addPrimitive(Entity e, AffineTransform transform, Color color) {
        if (e instanceof Hatch) {
            addHatch((Hatch) e, transform, color);
            return;
        }
        int pass = PASS_STROKE;
        Path2D.Double path = null;
        if (e instanceof Line) {
            Line l = (Line) e;
            path = new Path2D.Double();
            path.moveTo(l.start[0], l.start[1]);
            path.lineTo(l.end[0], l.end[1]);
        } else if (e instanceof Circle) {
            path = circlePath((Circle) e);
        } else if (e instanceof Ellipse) {
            path = ellipsePath((Ellipse) e);
        } else if (e instanceof LwPolyline) {
            path = polylinePath((LwPolyline) e);
        } else if (e instanceof Spline) {
            Spline s = (Spline) e;
            path = polygon(s.controlPoints.length > 0
                    ? Curves.nurbs(s.degree, s.knots, s.controlPoints, s.weights)
                    : Curves.through(s.fitPoints), false);
        } else if (e instanceof Leader) {
            List<double[]> points = new ArrayList<>();
            for (double[] p : ((Leader) e).points) {
                points.add(p);
            }
            path = polygon(points, false);
        } else if (e instanceof Point) {
            // a zero length segment : a dot of the line width
            Point p = (Point) e;
            path = new Path2D.Double();
            path.moveTo(p.position[0], p.position[1]);
            path.lineTo(p.position[0], p.position[1]);
        } else if (e instanceof Face3d) {
            path = facePath((Face3d) e);
        } else if (e instanceof PolyfaceMesh) {
            path = polyfacePath((PolyfaceMesh) e);
        } else if (e instanceof Solid) {
            Solid s = (Solid) e;
            List<double[]> points = new ArrayList<>();
            points.add(s.corners[0]);
            points.add(s.corners[1]);
            points.add(s.corners[3]);
            points.add(s.corners[2]);
            path = polygon(points, true);
            path.transform(new Ocs(s.extrusion).toAffine(s.elevation));
            pass = PASS_FILL;
        } else if (e instanceof Text) {
            path = textPath((Text) e);
            pass = PASS_TEXT;
        } else if (e instanceof MText) {
            path = mtextPath((MText) e);
            pass = PASS_TEXT;
        }
        if (path == null) {
            return;
        }
        path.transform(transform);
        addItem(pass, path, color);
    }

    private Item addItem(int pass, Shape shape, Color color) {
        Rectangle2D b = shape.getBounds2D();
        if (Double.isNaN(b.getX()) || Double.isNaN(b.getY()) || Double.isNaN(b.getWidth()) || Double.isNaN(b.getHeight())
                || Double.isInfinite(b.getX()) || Double.isInfinite(b.getY())
                || Double.isInfinite(b.getWidth()) || Double.isInfinite(b.getHeight())) {
            return null;
        }
        Item item = new Item();
        item.pass = pass;
        item.shape = shape;
        item.color = color;
        items.add(item);
        if (bounds == null) {
            bounds = (Rectangle2D) b.clone();
        } else {
            bounds.add(b);
        }
        return item;
    }

    private static Path2D.Double polygon(List<double[]> points, boolean close) {
        if (points.isEmpty()) {
            return null;
        }
        Path2D.Double path = new Path2D.Double();
        append(path, points, true);
        if (close) {
            path.closePath();
        }
        return path;
    }

    private static void append(Path2D path, List<double[]> points, boolean moveToFirst) {
        for (int i = 0; i < points.size(); i++) {
            double[] p = points.get(i);
            if (i == 0 && moveToFirst) {
                path.moveTo(p[0], p[1]);
            } else {
                path.lineTo(p[0], p[1]);
            }
        }
    }

    private Path2D.Double circlePath(Circle c) {
        double start = 0.0;
        double sweep = 2.0 * Math.PI;
        if (c instanceof Arc) {
            Arc a = (Arc) c;
            start = a.startAngle;
            sweep = Curves.ccwSweep(a.startAngle, a.endAngle);
        }
        Path2D.Double path = polygon(Curves.arc(c.center[0], c.center[1], c.radius, start, sweep), !(c instanceof Arc));
        path.transform(new Ocs(c.extrusion).toAffine(c.center[2]));
        return path;
    }

    // all in WCS : the minor axis is (extrusion x major axis) * ratio
    private Path2D.Double ellipsePath(Ellipse e) {
        double[] n = e.extrusion;
        double len = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
        if (len == 0.0) {
            n = new double[] { 0.0, 0.0, 1.0 };
            len = 1.0;
        }
        double[] m = e.majorAxis;
        double minorX = (n[1] * m[2] - n[2] * m[1]) / len * e.ratio;
        double minorY = (n[2] * m[0] - n[0] * m[2]) / len * e.ratio;
        double sweep = Curves.ccwSweep(e.startParam, e.endParam);
        boolean full = Math.abs(sweep - 2.0 * Math.PI) < 1e-9;
        return polygon(Curves.ellipse(e.center[0], e.center[1], m[0], m[1], minorX, minorY, e.startParam, sweep), full);
    }

    private Path2D.Double polylinePath(LwPolyline p) {
        int n = p.points.length;
        if (n == 0) {
            return null;
        }
        Path2D.Double path = new Path2D.Double();
        path.moveTo(p.points[0][0], p.points[0][1]);
        int segments = p.isClosed() ? n : n - 1;
        for (int i = 0; i < segments; i++) {
            double[] a = p.points[i];
            double[] b = p.points[(i + 1) % n];
            double bulge = i < p.bulges.length ? p.bulges[i] : 0.0;
            if (bulge == 0.0 || (a[0] == b[0] && a[1] == b[1])) {
                path.lineTo(b[0], b[1]);
            } else {
                append(path, Curves.bulge(a[0], a[1], b[0], b[1], bulge), false);
            }
        }
        path.transform(new Ocs(p.extrusion).toAffine(p.elevation));
        return path;
    }

    // edges of a 3DFACE; bit i of invisibleEdges hides the edge that starts at corner i
    private Path2D.Double facePath(Face3d f) {
        Path2D.Double path = new Path2D.Double();
        for (int i = 0; i < 4; i++) {
            double[] a = f.corners[i];
            double[] b = f.corners[(i + 1) % 4];
            if ((f.invisibleEdges & (1 << i)) == 0) {
                path.moveTo(a[0], a[1]);
                path.lineTo(b[0], b[1]);
            }
        }
        return path.getCurrentPoint() == null ? null : path;
    }

    // edges of the faces of a polyface mesh; a negative vertex index hides the edge that starts there
    private Path2D.Double polyfacePath(PolyfaceMesh m) {
        Path2D.Double path = new Path2D.Double();
        for (int[] face : m.faces) {
            int count = 0;
            while (count < 4 && face[count] != 0) {
                count++;
            }
            for (int i = 0; i < count && count > 1; i++) {
                int from = Math.abs(face[i]) - 1;
                int to = Math.abs(face[(i + 1) % count]) - 1;
                if (face[i] < 0 || from >= m.vertices.size() || to >= m.vertices.size()) {
                    continue;
                }
                double[] a = m.vertices.get(from);
                double[] b = m.vertices.get(to);
                path.moveTo(a[0], a[1]);
                path.lineTo(b[0], b[1]);
            }
        }
        return path.getCurrentPoint() == null ? null : path;
    }

    private Path2D.Double textPath(Text t) {
        String s = TextShapes.plain(t.text);
        if (s.trim().isEmpty() || !(t.height > 0.0)) {
            return null;
        }
        boolean atInsertion = (t.hAlign == 0 && t.vAlign == 0) || t.hAlign == 3 || t.hAlign == 5;
        double[] at = atInsertion ? t.insertion : t.alignment;
        Shape shape = TextShapes.aligned(s, t.height, t.widthFactor, t.oblique, atInsertion ? 0 : t.hAlign, atInsertion ? 0 : t.vAlign);
        AffineTransform place = new Ocs(t.extrusion).toAffine(t.elevation);
        place.translate(at[0], at[1]);
        place.rotate(t.rotation);
        Path2D.Double path = new Path2D.Double(shape);
        path.transform(place);
        return path;
    }

    private Path2D.Double mtextPath(MText m) {
        if (!(m.height > 0.0)) {
            return null;
        }
        Path2D.Double path = new Path2D.Double(TextShapes.paragraph(m.text, m.height, m.attachment, m.rectWidth));
        if (path.getCurrentPoint() == null) {
            return null;
        }
        AffineTransform place = AffineTransform.getTranslateInstance(m.insertion[0], m.insertion[1]);
        if (m.xAxis[0] != 0.0 || m.xAxis[1] != 0.0) {
            place.rotate(Math.atan2(m.xAxis[1], m.xAxis[0]));
        }
        path.transform(place);
        return path;
    }

    private void addHatch(Hatch h, AffineTransform transform, Color color) {
        Path2D.Double area = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        for (Hatch.Loop loop : h.loops) {
            List<double[]> points = loopPoints(loop);
            if (points.size() > 1) {
                append(area, points, true);
                area.closePath();
            }
        }
        if (area.getCurrentPoint() == null) {
            return;
        }
        AffineTransform toWorld = new AffineTransform(transform);
        toWorld.concatenate(new Ocs(h.extrusion).toAffine(h.elevation));
        Shape world = toWorld.createTransformedShape(area);
        if (h.solidFill) {
            addItem(PASS_FILL, world, color);
        } else if (h.pattern.isEmpty()) {
            addItem(PASS_STROKE, world, color);
        } else {
            Item item = addItem(PASS_PATTERN, world, color);
            if (item != null) {
                item.hatch = h;
                item.area = area;
                item.toWorld = toWorld;
            }
        }
    }

    private List<double[]> loopPoints(Hatch.Loop loop) {
        List<double[]> points = new ArrayList<>();
        if (loop.polyline) {
            int n = loop.vertices.length;
            for (int i = 0; i < n; i++) {
                double[] a = loop.vertices[i];
                double[] b = loop.vertices[(i + 1) % n];
                points.add(a);
                if (a[2] != 0.0 && (a[0] != b[0] || a[1] != b[1]) && (i < n - 1 || loop.closed)) {
                    List<double[]> arc = Curves.bulge(a[0], a[1], b[0], b[1], a[2]);
                    points.addAll(arc.subList(0, arc.size() - 1));
                }
            }
            return points;
        }
        for (Hatch.Edge e : loop.edges) {
            double[] v = e.v;
            switch (e.type) {
            case Hatch.Edge.LINE:
                points.add(new double[] { v[0], v[1] });
                points.add(new double[] { v[2], v[3] });
                break;
            case Hatch.Edge.ARC: {
                // a clockwise arc stores its angles mirrored about the X axis
                double sweep = Curves.ccwSweep(v[3], v[4]);
                points.addAll(Curves.arc(v[0], v[1], v[2], e.ccw ? v[3] : -v[3], e.ccw ? sweep : -sweep));
                break;
            }
            case Hatch.Edge.ELLIPSE: {
                double sweep = Curves.ccwSweep(v[5], v[6]);
                points.addAll(Curves.ellipse(v[0], v[1], v[2], v[3], -v[3] * v[4], v[2] * v[4],
                        e.ccw ? v[5] : -v[5], e.ccw ? sweep : -sweep));
                break;
            }
            default: {
                double[] weights = null;
                if (e.rational) {
                    weights = new double[e.controlPoints.length];
                    for (int i = 0; i < weights.length; i++) {
                        weights[i] = e.controlPoints[i][2];
                    }
                }
                points.addAll(Curves.nurbs(e.degree, e.knots, e.controlPoints, weights));
                break;
            }
            }
        }
        return points;
    }

    // pattern lines of a hatch, clipped to its boundary
    private void drawPattern(Graphics2D g, AffineTransform view, Item item) {
        AffineTransform full = new AffineTransform(view);
        full.concatenate(item.toWorld);
        double scale = Math.sqrt(Math.abs(full.getDeterminant()));      // pixels per OCS unit
        Rectangle2D b = item.area.getBounds2D();
        double[][] corners = {
            { b.getMinX(), b.getMinY() }, { b.getMaxX(), b.getMinY() },
            { b.getMaxX(), b.getMaxY() }, { b.getMinX(), b.getMaxY() } };

        Path2D.Double lines = new Path2D.Double();
        int dashes = 0;
        boolean tooDense = false;
        for (Hatch.PatternLine pl : item.hatch.pattern) {
            double dx = Math.cos(pl.angle);
            double dy = Math.sin(pl.angle);
            // spacing : the offset between two lines, measured across the line direction
            double spacing = -pl.offsetX * dy + pl.offsetY * dx;
            // shift : the same offset measured along the line direction; it staggers the dashes
            double shift = pl.offsetX * dx + pl.offsetY * dy;
            if (Math.abs(spacing) * scale < MIN_PATTERN_SPACING) {
                tooDense = true;
                break;
            }
            double minAcross = Double.MAX_VALUE, maxAcross = -Double.MAX_VALUE;
            double minAlong = Double.MAX_VALUE, maxAlong = -Double.MAX_VALUE;
            for (double[] c : corners) {
                double across = -(c[0] - pl.x) * dy + (c[1] - pl.y) * dx;
                double along = (c[0] - pl.x) * dx + (c[1] - pl.y) * dy;
                minAcross = Math.min(minAcross, across);
                maxAcross = Math.max(maxAcross, across);
                minAlong = Math.min(minAlong, along);
                maxAlong = Math.max(maxAlong, along);
            }
            long first = (long) Math.ceil(Math.min(minAcross / spacing, maxAcross / spacing));
            long last = (long) Math.floor(Math.max(minAcross / spacing, maxAcross / spacing));
            if (last - first > MAX_PATTERN_LINES) {
                tooDense = true;
                break;
            }
            double period = 0.0;
            for (double d : pl.dashes) {
                period += Math.abs(d);
            }
            boolean continuous = period * scale < 2.0;      // no dashes, or dashes too fine to see
            for (long k = first; k <= last && !tooDense; k++) {
                // the line through (ox, oy); positions along it are measured from that point
                double ox = pl.x + k * pl.offsetX;
                double oy = pl.y + k * pl.offsetY;
                double from = minAlong - k * shift;
                double to = maxAlong - k * shift;
                if (continuous) {
                    lines.moveTo(ox + from * dx, oy + from * dy);
                    lines.lineTo(ox + to * dx, oy + to * dy);
                    continue;
                }
                double t = Math.floor(from / period) * period;
                while (t < to && !tooDense) {
                    for (double d : pl.dashes) {
                        if (d >= 0.0) {
                            lines.moveTo(ox + t * dx, oy + t * dy);
                            lines.lineTo(ox + (t + d) * dx, oy + (t + d) * dy);
                            tooDense = ++dashes > MAX_PATTERN_DASHES;
                        }
                        t += Math.abs(d);
                    }
                }
            }
            if (tooDense) {
                break;
            }
        }

        Shape device = view.createTransformedShape(item.shape);
        if (tooDense) {
            g.setColor(new Color(item.color.getRed(), item.color.getGreen(), item.color.getBlue(), DENSE_PATTERN_ALPHA));
            g.fill(device);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.clip(device);
        g2.draw(full.createTransformedShape(lines));
        g2.dispose();
    }

    /** AutoCAD color index to RGB. Indexes 10-249 are approximated from the hue/shade pattern of the table. */
    private Color colorOf(int aci) {
        switch (aci) {
        case 1: return new Color(255, 0, 0);
        case 2: return new Color(255, 255, 0);
        case 3: return new Color(0, 255, 0);
        case 4: return new Color(0, 255, 255);
        case 5: return new Color(0, 0, 255);
        case 6: return new Color(255, 0, 255);
        case 8: return new Color(128, 128, 128);
        case 9: return new Color(192, 192, 192);
        default: break;
        }
        if (aci >= 10 && aci <= 249) {
            float hue = (aci / 10 - 1) * 15f / 360f;
            float[] shades = { 1.0f, 0.74f, 0.505f, 0.41f, 0.31f };
            float value = shades[aci % 10 / 2];
            float saturation = aci % 2 == 0 ? 1.0f : 0.333f;
            return Color.getHSBColor(hue, saturation, value);
        }
        if (aci >= 250 && aci <= 255) {
            int v = 51 + (aci - 250) * 36;
            return new Color(v, v, v);
        }
        return foreground;      // 7 and anything unresolved
    }

    /** Object coordinate system of an extrusion direction (arbitrary axis algorithm), projected to the X/Y plane. */
    private static class Ocs {
        private final boolean identity;
        private final double axX, axY, ayX, ayY, nX, nY;

        Ocs(double[] n) {
            if (n == null || (n[0] == 0.0 && n[1] == 0.0 && n[2] >= 0.0)) {
                identity = true;
                axX = axY = ayX = ayY = nX = nY = 0.0;
                return;
            }
            identity = false;
            double len = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
            double x = n[0] / len, y = n[1] / len, z = n[2] / len;
            double[] ax;
            if (Math.abs(x) < 1.0 / 64.0 && Math.abs(y) < 1.0 / 64.0) {
                ax = new double[] { z, 0.0, -x };               // Wy x N
            } else {
                ax = new double[] { -y, x, 0.0 };               // Wz x N
            }
            double al = Math.sqrt(ax[0] * ax[0] + ax[1] * ax[1] + ax[2] * ax[2]);
            ax[0] /= al; ax[1] /= al; ax[2] /= al;
            axX = ax[0];
            axY = ax[1];
            ayX = y * ax[2] - z * ax[1];                        // Ay = N x Ax
            ayY = z * ax[0] - x * ax[2];
            nX = x;
            nY = y;
        }

        /** OCS (x, y) at the given OCS z -> X/Y */
        AffineTransform toAffine(double z) {
            if (identity) {
                return new AffineTransform();
            }
            return new AffineTransform(axX, axY, ayX, ayY, z * nX, z * nY);
        }
    }
}
