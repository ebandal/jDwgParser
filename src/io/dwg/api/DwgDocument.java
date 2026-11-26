package io.dwg.api;

import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgEntity;
import io.dwg.entities.DwgObject;
import io.dwg.entities.concrete.DwgLayer;
import io.dwg.entities.concrete.DwgLtype;
import io.dwg.entities.concrete.DwgStyle;
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
    private Map<Long, DwgObject> objectMap = new ObjectsByHandle(Collections.emptyMap());
    private HandleRegistry handleRegistry;
    private DwgClassRegistry classRegistry;
    private List<DwgClassDefinition> customClasses = new ArrayList<>();
    private List<ReadIssue> issues = new ArrayList<>();
    private Map<Long, byte[]> dataStorage = Collections.emptyMap();

    public DwgDocument(DwgVersion version) {
        this.version = version;
    }

    public DwgVersion version() { return version; }

    public HeaderVariables header() { return headerVariables; }
    public void setHeaderVariables(HeaderVariables h) { this.headerVariables = h; }

    /** The objects by handle, read-only, in handle order. */
    public Map<Long, DwgObject> objectMap() { return objectMap; }
    public void setObjectMap(Map<Long, DwgObject> m) { this.objectMap = new ObjectsByHandle(m); }

    public HandleRegistry handleRegistry() { return handleRegistry; }
    public void setHandleRegistry(HandleRegistry h) { this.handleRegistry = h; }

    public DwgClassRegistry classRegistry() { return classRegistry; }
    /** The problems met while reading, in order (at most 10,000; see {@link DwgReader#onIssue}). */
    public List<ReadIssue> issues() { return Collections.unmodifiableList(issues); }
    public void setIssues(List<ReadIssue> issues) { this.issues = issues; }

    /**
     * R2013+: the data records of the data storage section (AcDb:AcDsPrototype_1b) by object
     * handle, such as the SAB models of ACIS entities; empty for other versions.
     */
    public Map<Long, byte[]> dataStorage() { return Collections.unmodifiableMap(dataStorage); }
    public void setDataStorage(Map<Long, byte[]> records) { this.dataStorage = records; }
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

    /**
     * The objects of one type (or of its subtypes), in the order of their handles, e.g.
     * {@code objects(DwgLine.class)} or {@code objects(DwgEntity.class)}.
     */
    public <T extends DwgObject> List<T> objects(Class<T> type) {
        return objectMap.values().stream()
            .filter(type::isInstance)
            .sorted(java.util.Comparator.comparingLong(DwgObject::handle))
            .map(type::cast)
            .collect(Collectors.toList());
    }

    /**
     * The text of the drawing as plain text ({@link PlainText}), in the order of the handles:
     * TEXT, ATTRIB and MTEXT, the cells of tables and the text of multileaders. Empty texts are
     * left out. For search and indexing; it covers the model space, the layouts and the blocks.
     */
    public List<String> texts() {
        List<String> texts = new ArrayList<>();
        for (DwgObject o : objects(DwgObject.class)) {
            if (o instanceof io.dwg.entities.concrete.DwgText) {
                add(texts, PlainText.ofText(((io.dwg.entities.concrete.DwgText) o).value()));
            } else if (o instanceof io.dwg.entities.concrete.DwgAttrib) {
                add(texts, PlainText.ofText(((io.dwg.entities.concrete.DwgAttrib) o).text()));
            } else if (o instanceof io.dwg.entities.concrete.DwgMText) {
                add(texts, PlainText.ofMText(((io.dwg.entities.concrete.DwgMText) o).text()));
            } else if (o instanceof io.dwg.entities.concrete.DwgMLeader) {
                add(texts, PlainText.ofMText(((io.dwg.entities.concrete.DwgMLeader) o).text()));
            } else if (o instanceof io.dwg.entities.concrete.DwgTable) {
                for (List<String> row : ((io.dwg.entities.concrete.DwgTable) o).cellTexts()) {
                    for (String cell : row) {
                        add(texts, PlainText.ofMText(cell));
                    }
                }
            }
        }
        return texts;
    }

    private static void add(List<String> texts, String text) {
        if (text != null && !text.trim().isEmpty()) {
            texts.add(text);
        }
    }

    /** The object a handle points at, if the file has it; empty for a null or missing handle. */
    public Optional<DwgObject> object(io.dwg.core.type.DwgHandleRef ref) {
        if (ref == null || ref.isNull()) {
            return Optional.empty();
        }
        return Optional.ofNullable(objectMap.get(ref.rawHandle()));
    }

    /** The layer of an entity, if its layer handle points at a layer of the file. */
    public Optional<DwgLayer> layerOf(DwgEntity entity) {
        return object(entity.layerHandle()).filter(o -> o instanceof DwgLayer).map(o -> (DwgLayer) o);
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

    /** 선종류 목록 */
    public List<DwgLtype> linetypes() {
        return objectMap.values().stream()
            .filter(o -> o instanceof DwgLtype)
            .map(o -> (DwgLtype) o)
            .collect(Collectors.toList());
    }

    /** 이름으로 선종류 조회 */
    public Optional<DwgLtype> linetype(String name) {
        return linetypes().stream().filter(lt -> lt.name().equals(name)).findFirst();
    }

    /** 텍스트 스타일 목록 */
    public List<DwgStyle> styles() {
        return objectMap.values().stream()
            .filter(o -> o instanceof DwgStyle)
            .map(o -> (DwgStyle) o)
            .collect(Collectors.toList());
    }

    /** 이름으로 텍스트 스타일 조회 */
    public Optional<DwgStyle> style(String name) {
        return styles().stream().filter(s -> s.name().equals(name)).findFirst();
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

    /** 테이블 접근 API 제공 */
    public DwgTableLocator tables() {
        return new DwgTableLocator(this);
    }
}
