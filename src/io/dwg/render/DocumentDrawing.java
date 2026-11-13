package io.dwg.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.dwg.api.DwgDocument;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point2D;
import io.dwg.core.type.Point3D;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.DwgDimension;
import io.dwg.entities.concrete.DwgArc;
import io.dwg.entities.concrete.DwgAttrib;
import io.dwg.entities.concrete.DwgBlockHeader;
import io.dwg.entities.concrete.DwgCircle;
import io.dwg.entities.concrete.DwgEllipse;
import io.dwg.entities.concrete.DwgFace3D;
import io.dwg.entities.concrete.DwgHatch;
import io.dwg.entities.concrete.DwgInsert;
import io.dwg.entities.concrete.DwgLayer;
import io.dwg.entities.concrete.DwgLeader;
import io.dwg.entities.concrete.DwgLine;
import io.dwg.entities.concrete.DwgLwPolyline;
import io.dwg.entities.concrete.DwgMText;
import io.dwg.entities.concrete.DwgPoint;
import io.dwg.entities.concrete.DwgPolylinePface;
import io.dwg.entities.concrete.DwgSolid;
import io.dwg.entities.concrete.DwgSpline;
import io.dwg.entities.concrete.DwgText;
import io.dwg.entities.concrete.DwgTrace;
import io.dwg.entities.concrete.DwgVertexPface;
import io.dwg.entities.concrete.DwgVertexPfaceFace;
import io.dwg.render.model.BlockHeader;
import io.dwg.render.model.Drawing;
import io.dwg.render.model.DwgObject;
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
import io.dwg.render.model.entity.PolyfaceVertex;
import io.dwg.render.model.entity.Solid;
import io.dwg.render.model.entity.Spline;
import io.dwg.render.model.entity.Text;

/**
 * Builds what the {@link Renderer} draws from a {@link DwgDocument} : layers, blocks and the entities of the
 * model space and of each block.
 *
 * Entities of a type the renderer does not draw are left out. An entity of a drawable type whose data
 * DwgReader could not read is left out too and counted in {@link Drawing#failedCount}.
 */
public final class DocumentDrawing {

    private DocumentDrawing() {
    }

    public static Drawing from(DwgDocument doc) {
        Drawing d = new Drawing();

        // objects in handle order : sub-entities follow the entity they belong to
        List<io.dwg.entities.DwgObject> objects = new ArrayList<>(doc.objectMap().values());
        objects.sort((a, b) -> Long.compare(a.handle(), b.handle()));

        List<Entity> entities = new ArrayList<>();
        Map<Long, Entity> byHandle = new HashMap<>();
        for (io.dwg.entities.DwgObject o : objects) {
            if (o instanceof DwgLayer) {
                d.layers.put(o.handle(), layer((DwgLayer) o));
            } else if (o instanceof DwgBlockHeader) {
                BlockHeader b = block((DwgBlockHeader) o);
                if (b != null) {
                    d.blocks.put(o.handle(), b);
                } else {
                    d.failedCount++;
                }
            } else if (o instanceof AbstractDwgEntity && ((AbstractDwgEntity) o).layerHandle() != null) {
                // an entity always has a layer; classes that only extend the entity base class do not
                d.entityCount++;
                AbstractDwgEntity a = (AbstractDwgEntity) o;
                Entity e;
                try {
                    e = entity(a);
                } catch (RuntimeException ex) {
                    // a drawable type whose data is missing or unusable
                    d.failedCount++;
                    continue;
                }
                if (e == null) {
                    continue;
                }
                common(e, a);
                d.decodedCount++;
                entities.add(e);
                byHandle.put(o.handle(), e);
            }
        }

        // Sub-entities belong to the entity that precedes them : attributes to their block reference,
        // vertices to their polyline. When the owner handle names that entity, it is used instead.
        Entity parent = null;
        for (Entity e : entities) {
            boolean attrib = e instanceof Text && ((Text) e).attribute;
            if (attrib || e instanceof PolyfaceVertex) {
                Entity owner = e.entMode == Entity.MODE_OWNED ? byHandle.get(e.ownerHandle) : null;
                if (!(owner instanceof Insert) && !(owner instanceof PolyfaceMesh)) {
                    owner = parent;
                }
                if (attrib && owner instanceof Insert) {
                    ((Insert) owner).attribs.add((Text) e);
                } else if (!attrib && owner instanceof PolyfaceMesh) {
                    PolyfaceVertex v = (PolyfaceVertex) e;
                    if (v.point != null) {
                        ((PolyfaceMesh) owner).vertices.add(v.point);
                    } else {
                        ((PolyfaceMesh) owner).faces.add(v.indexes);
                    }
                }
                continue;
            }
            if (e instanceof Insert || e instanceof PolyfaceMesh) {
                parent = e;
            }
            if (e.entMode == Entity.MODE_MODEL_SPACE) {
                d.modelSpace.add(e);
            } else if (e.entMode == Entity.MODE_OWNED) {
                d.blockEntities.computeIfAbsent(e.ownerHandle, k -> new ArrayList<>()).add(e);
            }
        }

        // entities that name the model space block as their owner belong to the model space too
        for (BlockHeader b : d.blocks.values()) {
            if (b.isModelSpace() && d.blockEntities.containsKey(b.handle)) {
                d.modelSpace.addAll(d.blockEntities.remove(b.handle));
            }
        }
        return d;
    }

    private static Layer layer(DwgLayer o) {
        Layer l = new Layer();
        l.handle = o.handle();
        l.name = o.name() == null ? "" : o.name();
        l.color = o.color() == null ? Entity.COLOR_BYLAYER : o.color().getColorIndex();
        l.frozen = o.isFrozen();
        l.off = !o.isOn();
        return l;
    }

    private static BlockHeader block(DwgBlockHeader o) {
        if (o.basePoint() == null) {
            return null;
        }
        BlockHeader b = new BlockHeader();
        b.handle = o.handle();
        b.name = o.blockName() == null ? "" : o.blockName();
        b.anonymous = o.isAnonymous();
        b.xref = o.isXref() || o.isXrefOverlay();
        b.basePoint = xyz(o.basePoint());
        return b;
    }

    // values every entity has
    private static void common(Entity e, AbstractDwgEntity a) {
        DwgObject object = new DwgObject();
        object.handle = a.handle();
        object.type = a.rawTypeCode();
        object.typeName = a.objectType().name();
        object.entity = true;
        e.object = object;
        e.entMode = a.entityMode();
        e.color = a.color() == null ? Entity.COLOR_BYLAYER : a.color().getColorIndex();
        e.ltypeScale = a.lineTypeScale();
        e.invisible = (a.invisibility() & 1) != 0;
        e.lineweight = (int) a.lineWeight();
        e.ownerHandle = handle(a.ownerHandle());
        e.layerHandle = handle(a.layerHandle());
    }

    /** @return the entity for the renderer, or null if the renderer does not draw this type */
    private static Entity entity(AbstractDwgEntity o) {
        if (o instanceof DwgLine) {
            DwgLine s = (DwgLine) o;
            Line e = new Line();
            e.start = xyz(s.start());
            e.end = xyz(s.end());
            e.thickness = s.thickness();
            e.extrusion = s.extrusion();
            return e;
        }
        if (o instanceof DwgArc) {
            DwgArc s = (DwgArc) o;
            Arc e = new Arc();
            e.center = xyz(s.center());
            e.radius = s.radius();
            e.thickness = s.thickness();
            e.extrusion = s.extrusion();
            e.startAngle = s.startAngle();
            e.endAngle = s.endAngle();
            return e;
        }
        if (o instanceof DwgCircle) {
            DwgCircle s = (DwgCircle) o;
            Circle e = new Circle();
            e.center = xyz(s.center());
            e.radius = s.radius();
            e.thickness = s.thickness();
            e.extrusion = s.extrusion();
            return e;
        }
        if (o instanceof DwgEllipse) {
            DwgEllipse s = (DwgEllipse) o;
            Ellipse e = new Ellipse();
            e.center = xyz(s.center());
            e.majorAxis = xyz(s.majorAxisVec());
            e.extrusion = required(s.extrusion());
            e.ratio = s.axisRatio();
            e.startParam = s.startParam();
            e.endParam = s.endParam();
            return e;
        }
        if (o instanceof DwgLwPolyline) {
            DwgLwPolyline s = (DwgLwPolyline) o;
            LwPolyline e = new LwPolyline();
            e.flag = s.flags();
            e.constWidth = s.constantWidth();
            e.elevation = s.elevation();
            e.thickness = s.thickness();
            e.extrusion = required(s.extrusion());
            e.points = new double[s.vertices().size()][];
            for (int i = 0; i < e.points.length; i++) {
                e.points[i] = xy(s.vertices().get(i));
            }
            e.bulges = new double[s.bulges().size()];
            for (int i = 0; i < e.bulges.length; i++) {
                e.bulges[i] = s.bulges().get(i);
            }
            e.widths = s.widths().toArray(new double[0][]);
            return e;
        }
        if (o instanceof DwgSpline) {
            DwgSpline s = (DwgSpline) o;
            Spline e = new Spline();
            e.degree = s.degree();
            e.rational = s.isRational();
            e.closed = s.isClosed();
            e.periodic = s.isPeriodic();
            e.knots = doubles(s.knots());
            e.weights = doubles(s.weights());
            e.controlPoints = points(s.controlPoints());
            e.fitPoints = points(s.fitPoints());
            return e;
        }
        if (o instanceof DwgText) {
            return text((DwgText) o);
        }
        if (o instanceof DwgAttrib) {
            DwgAttrib s = (DwgAttrib) o;
            Text e = text(s.textData());
            e.attribute = true;
            e.tag = s.tag();
            e.attributeFlags = s.flags();
            return e;
        }
        if (o instanceof DwgMText) {
            DwgMText s = (DwgMText) o;
            MText e = new MText();
            e.insertion = xyz(s.location());
            e.extrusion = s.extrusion();
            e.xAxis = s.xAxisDirection() == null ? new double[] { 1.0, 0.0, 0.0 } : xyz(s.xAxisDirection());
            e.rectWidth = s.width();
            e.height = s.height();
            e.attachment = s.attachmentPoint();
            e.text = required(s.text());
            return e;
        }
        if (o instanceof DwgPoint) {
            Point e = new Point();
            e.position = xyz(((DwgPoint) o).position());
            return e;
        }
        if (o instanceof DwgFace3D) {
            DwgFace3D s = (DwgFace3D) o;
            Face3d e = new Face3d();
            for (int i = 0; i < 4; i++) {
                e.corners[i] = xyz(s.points()[i]);
            }
            e.invisibleEdges = s.invisibleEdges();
            return e;
        }
        if (o instanceof DwgSolid) {
            return solid(((DwgSolid) o).points(), ((DwgSolid) o).extrusion());
        }
        if (o instanceof DwgTrace) {
            return solid(((DwgTrace) o).points(), ((DwgTrace) o).extrusion());
        }
        if (o instanceof DwgLeader) {
            DwgLeader s = (DwgLeader) o;
            Leader e = new Leader();
            e.pathType = s.pathType();
            e.points = points(s.points());
            e.arrowhead = s.isArrowheadOn();
            return e;
        }
        if (o instanceof DwgInsert) {
            DwgInsert s = (DwgInsert) o;
            Insert e = new Insert();
            e.insertion = xyz(s.insertionPoint());
            e.scale = new double[] { s.xScale(), s.yScale(), s.zScale() };
            e.rotation = s.rotation();
            e.extrusion = s.extrusion();
            e.hasAttribs = s.hasAttribs();
            e.blockHandle = handle(s.blockHeaderHandle());
            return e;
        }
        if (o instanceof DwgDimension) {
            Dimension e = new Dimension();
            e.blockHandle = handle(((DwgDimension) o).blockHandle());
            return e;
        }
        if (o instanceof DwgPolylinePface) {
            DwgPolylinePface s = (DwgPolylinePface) o;
            PolyfaceMesh e = new PolyfaceMesh();
            e.numVertices = s.numVertices();
            e.numFaces = s.numFaces();
            return e;
        }
        if (o instanceof DwgVertexPface) {
            PolyfaceVertex e = new PolyfaceVertex();
            e.point = xyz(((DwgVertexPface) o).location());
            return e;
        }
        if (o instanceof DwgVertexPfaceFace) {
            DwgVertexPfaceFace s = (DwgVertexPfaceFace) o;
            PolyfaceVertex e = new PolyfaceVertex();
            e.indexes = new int[] { s.vertex1Index(), s.vertex2Index(), s.vertex3Index(), s.vertex4Index() };
            return e;
        }
        if (o instanceof DwgHatch) {
            return hatch((DwgHatch) o);
        }
        return null;
    }

    private static Text text(DwgText s) {
        Text e = new Text();
        e.elevation = s.elevation();
        e.insertion = xy(s.insertionPoint());
        // the alignment point is stored only when it differs from the insertion point
        e.alignment = s.alignmentPoint() == null ? e.insertion.clone() : xy(s.alignmentPoint());
        e.extrusion = s.extrusion();
        e.thickness = s.thickness();
        e.oblique = s.obliquAngle();
        e.rotation = s.rotationAngle();
        e.height = s.height();
        e.widthFactor = s.widthFactor();
        e.text = required(s.value());
        e.generation = s.generation();
        e.hAlign = s.horizontalAlignment();
        e.vAlign = s.verticalAlignment();
        return e;
    }

    // the corners carry the elevation as their z
    private static Solid solid(Point3D[] p, double[] extrusion) {
        Solid e = new Solid();
        for (int i = 0; i < 4; i++) {
            e.corners[i] = new double[] { p[i].x(), p[i].y() };
        }
        e.elevation = p[0].z();
        e.extrusion = extrusion;
        return e;
    }

    private static Hatch hatch(DwgHatch s) {
        Hatch e = new Hatch();
        e.elevation = s.elevation();
        e.extrusion = required(s.extrusion());
        e.name = s.patternName();
        e.solidFill = s.isSolidFill();
        for (DwgHatch.Loop sl : s.loops()) {
            Hatch.Loop l = new Hatch.Loop();
            l.flag = sl.flags;
            l.polyline = sl.polyline;
            l.closed = sl.closed;
            l.vertices = sl.vertices.toArray(new double[0][]);
            for (DwgHatch.Edge se : sl.edges) {
                Hatch.Edge edge = new Hatch.Edge();
                edge.type = se.type;
                edge.v = se.values;
                edge.ccw = se.counterClockwise;
                edge.degree = se.degree;
                edge.rational = se.rational;
                edge.knots = se.knots;
                edge.controlPoints = se.controlPoints;
                l.edges.add(edge);
            }
            e.loops.add(l);
        }
        for (DwgHatch.PatternLine sp : s.patternLines()) {
            Hatch.PatternLine p = new Hatch.PatternLine();
            p.angle = sp.angle;
            p.x = sp.x;
            p.y = sp.y;
            p.offsetX = sp.offsetX;
            p.offsetY = sp.offsetY;
            p.dashes = sp.dashes;
            e.pattern.add(p);
        }
        return e;
    }

    private static long handle(DwgHandleRef ref) {
        return ref == null ? 0L : ref.rawHandle();
    }

    private static <T> T required(T value) {
        if (value == null) {
            throw new IllegalStateException("value missing");
        }
        return value;
    }

    private static double[] xyz(Point3D p) {
        required(p);
        return new double[] { p.x(), p.y(), p.z() };
    }

    private static double[] xy(Point2D p) {
        required(p);
        return new double[] { p.x(), p.y() };
    }

    private static double[][] points(List<Point3D> list) {
        double[][] result = new double[list.size()][];
        for (int i = 0; i < result.length; i++) {
            result[i] = xyz(list.get(i));
        }
        return result;
    }

    private static double[] doubles(List<Double> list) {
        double[] result = new double[list.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = list.get(i);
        }
        return result;
    }
}
