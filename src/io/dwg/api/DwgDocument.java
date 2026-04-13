package io.dwg.api;

import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgEntity;
import io.dwg.entities.DwgObject;
import io.dwg.entities.concrete.DwgLayer;
import io.dwg.sections.classes.DwgClassDefinition;
import io.dwg.sections.classes.DwgClassRegistry;
import io.dwg.sections.handles.HandleRegistry;
import io.dwg.sections.header.HeaderVariables;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 파싱된 DWG 파일 전체를 나타내는 루트 객체.
 */
public class DwgDocument {
    private final DwgVersion version;
    private HeaderVariables headerVariables;
    private Map<Long, DwgObject> objectMap = new HashMap<>();
    private HandleRegistry handleRegistry;
    private DwgClassRegistry classRegistry;
    private List<DwgClassDefinition> customClasses = new ArrayList<>();

    public DwgDocument(DwgVersion version) {
        this.version = version;
    }

    public DwgVersion version() { return version; }

    public HeaderVariables header() { return headerVariables; }
    public void setHeaderVariables(HeaderVariables h) { this.headerVariables = h; }

    public Map<Long, DwgObject> objectMap() { return Collections.unmodifiableMap(objectMap); }
    public void setObjectMap(Map<Long, DwgObject> m) { this.objectMap = m; }

    public HandleRegistry handleRegistry() { return handleRegistry; }
    public void setHandleRegistry(HandleRegistry h) { this.handleRegistry = h; }

    public DwgClassRegistry classRegistry() { return classRegistry; }
    public void setClassRegistry(DwgClassRegistry r) { this.classRegistry = r; }

    public List<DwgClassDefinition> customClasses() { return customClasses; }
    public void setCustomClasses(List<DwgClassDefinition> list) { this.customClasses = list; }

    /** 전체 엔티티 목록 */
    public List<DwgEntity> entities() {
        return objectMap.values().stream()
            .filter(DwgObject::isEntity)
            .map(o -> (DwgEntity) o)
            .collect(Collectors.toList());
    }

    /** 레이어 목록 */
    public List<DwgLayer> layers() {
        return objectMap.values().stream()
            .filter(o -> o instanceof DwgLayer)
            .map(o -> (DwgLayer) o)
            .collect(Collectors.toList());
    }

    /** 이름으로 레이어 조회 */
    public Optional<DwgLayer> layer(String name) {
        return layers().stream().filter(l -> l.name().equals(name)).findFirst();
    }

    /** 핸들로 객체 조회 */
    public <T extends DwgObject> Optional<T> objectByHandle(long handle, Class<T> type) {
        DwgObject obj = objectMap.get(handle);
        if (obj != null && type.isInstance(obj)) return Optional.of(type.cast(obj));
        return Optional.empty();
    }

    /** 지정 타입 객체 목록 */
    public <T extends DwgObject> List<T> objectsOfType(Class<T> type) {
        return objectMap.values().stream()
            .filter(type::isInstance)
            .map(type::cast)
            .collect(Collectors.toList());
    }
}
