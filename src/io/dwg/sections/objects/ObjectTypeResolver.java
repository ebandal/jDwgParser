package io.dwg.sections.objects;

import io.dwg.sections.classes.DwgClassRegistry;
import io.dwg.sections.objects.readers.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 객체 타입 번호 → ObjectReader 인스턴스 매핑.
 */
public class ObjectTypeResolver {
    private final Map<Integer, ObjectReader> readers = new HashMap<>();

    public void register(ObjectReader reader) {
        readers.put(reader.objectType(), reader);
    }

    public void registerCustom(int classNum, ObjectReader reader) {
        readers.put(classNum, reader);
    }

    public Optional<ObjectReader> resolve(int typeCode) {
        return Optional.ofNullable(readers.get(typeCode));
    }

    public static ObjectTypeResolver defaultResolver(DwgClassRegistry classReg) {
        ObjectTypeResolver resolver = new ObjectTypeResolver();
        resolver.register(new LineObjectReader());
        resolver.register(new CircleObjectReader());
        resolver.register(new ArcObjectReader());
        resolver.register(new TextObjectReader());
        resolver.register(new InsertObjectReader());
        resolver.register(new LayerObjectReader());
        return resolver;
    }
}
