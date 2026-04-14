package io.dwg.sections.objects;

import io.dwg.sections.objects.writers.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 객체 타입 번호 → ObjectWriter 인스턴스 매핑.
 */
public class ObjectWriterRegistry {
    private final Map<Integer, ObjectWriter> writers = new HashMap<>();

    public void register(ObjectWriter writer) {
        writers.put(writer.objectType(), writer);
    }

    public Optional<ObjectWriter> resolve(int typeCode) {
        return Optional.ofNullable(writers.get(typeCode));
    }

    public static ObjectWriterRegistry defaultRegistry() {
        ObjectWriterRegistry registry = new ObjectWriterRegistry();
        registry.register(new LineObjectWriter());
        registry.register(new CircleObjectWriter());
        registry.register(new ArcObjectWriter());
        registry.register(new TextObjectWriter());
        registry.register(new InsertObjectWriter());
        registry.register(new PointObjectWriter());
        registry.register(new EllipseObjectWriter());
        registry.register(new SolidObjectWriter());
        registry.register(new TraceObjectWriter());
        registry.register(new Face3DObjectWriter());
        registry.register(new SeqEndObjectWriter());
        registry.register(new Vertex2DObjectWriter());
        registry.register(new Vertex3DObjectWriter());
        registry.register(new Polyline2DObjectWriter());
        registry.register(new Polyline3DObjectWriter());
        registry.register(new RayObjectWriter());
        registry.register(new XLineObjectWriter());
        registry.register(new LwPolylineObjectWriter());
        registry.register(new ShapeObjectWriter());
        registry.register(new MTextObjectWriter());
        registry.register(new AttribObjectWriter());
        registry.register(new AttdefObjectWriter());
        registry.register(new MinsertObjectWriter());
        registry.register(new BlockHeaderObjectWriter());
        registry.register(new BlockEndObjectWriter());
        return registry;
    }
}
