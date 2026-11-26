package io.dwg.entities;

import io.dwg.core.type.DwgHandleRef;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DwgObject의 공통 필드 구현. 모든 구체 클래스의 부모.
 * <p>Fields that few objects have (reactors, named values, raw data) are kept together in one
 * object made when the first of them is set, so the others do not pay for them.
 */
public abstract class AbstractDwgObject implements DwgObject, ValueHolder {
    protected long handle;
    protected DwgHandleRef ownerHandle;
    protected DwgHandleRef xDicHandle;
    protected List<XDataRecord> xData;                   // null until the first record
    protected int rawTypeCode;
    private Rare rare;                                  // null until a rare field is set

    /** The fields few objects have. */
    private static final class Rare {
        List<DwgHandleRef> reactorHandles;
        Map<String, Object> values;
        byte[] rawData;
    }

    private Rare rare() {
        if (rare == null) {
            rare = new Rare();
        }
        return rare;
    }

    @Override public long handle() { return handle; }
    @Override public int rawTypeCode() { return rawTypeCode; }
    @Override public DwgHandleRef ownerHandle() { return ownerHandle; }
    @Override public List<DwgHandleRef> reactorHandles() {
        return rare == null || rare.reactorHandles == null ? Collections.emptyList() : Collections.unmodifiableList(rare.reactorHandles);
    }
    @Override public Optional<DwgHandleRef> xDicHandle() { return Optional.ofNullable(xDicHandle); }
    @Override public List<XDataRecord> xData() { return xData == null ? Collections.emptyList() : Collections.unmodifiableList(xData); }

    public void setHandle(long handle) { this.handle = handle; }
    public void setOwnerHandle(DwgHandleRef ownerHandle) { this.ownerHandle = ownerHandle; }
    public void setRawTypeCode(int rawTypeCode) { this.rawTypeCode = rawTypeCode; }
    public void addReactorHandle(DwgHandleRef h) {
        Rare r = rare();
        if (r.reactorHandles == null) r.reactorHandles = new ArrayList<>(2);
        r.reactorHandles.add(h);
    }
    public void setXDicHandle(DwgHandleRef xDicHandle) { this.xDicHandle = xDicHandle; }
    public void addXData(XDataRecord record) { if (xData == null) xData = new ArrayList<>(4); xData.add(record); }

    /**
     * The object's data as stored, from the type code to the end, for objects whose data this
     * library does not read (unknown classes, proxies, types the specification does not
     * describe); {@code null} for the others.
     */
    public byte[] rawData() { return rare == null ? null : rare.rawData; }

    public void setRawData(byte[] rawData) {
        if (rawData != null || rare != null) rare().rawData = rawData;
    }

    /**
     * Fields kept under names (see {@link ValueHolder}); used by the readers of objects with many
     * fields. Empty for most objects.
     */
    @Override
    public Map<String, Object> values() {
        return rare == null || rare.values == null ? Collections.emptyMap() : Collections.unmodifiableMap(rare.values);
    }

    @Override
    public void setValue(String name, Object value) {
        Rare r = rare();
        if (r.values == null) r.values = new LinkedHashMap<>();
        r.values.put(name, value);
    }
    /** Replaces each XData record by what {@code change} returns for it. */
    public void replaceXData(java.util.function.UnaryOperator<XDataRecord> change) { if (xData != null) xData.replaceAll(change); }
}
