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
 */
public abstract class AbstractDwgObject implements DwgObject, ValueHolder {
    protected long handle;
    protected DwgHandleRef ownerHandle;
    protected List<DwgHandleRef> reactorHandles = new ArrayList<>();
    protected DwgHandleRef xDicHandle;
    protected List<XDataRecord> xData = new ArrayList<>();
    protected int rawTypeCode;
    private Map<String, Object> values;
    private byte[] rawData;

    @Override public long handle() { return handle; }
    @Override public int rawTypeCode() { return rawTypeCode; }
    @Override public DwgHandleRef ownerHandle() { return ownerHandle; }
    @Override public List<DwgHandleRef> reactorHandles() { return Collections.unmodifiableList(reactorHandles); }
    @Override public Optional<DwgHandleRef> xDicHandle() { return Optional.ofNullable(xDicHandle); }
    @Override public List<XDataRecord> xData() { return Collections.unmodifiableList(xData); }

    public void setHandle(long handle) { this.handle = handle; }
    public void setOwnerHandle(DwgHandleRef ownerHandle) { this.ownerHandle = ownerHandle; }
    public void setRawTypeCode(int rawTypeCode) { this.rawTypeCode = rawTypeCode; }
    public void addReactorHandle(DwgHandleRef h) { this.reactorHandles.add(h); }
    public void setXDicHandle(DwgHandleRef xDicHandle) { this.xDicHandle = xDicHandle; }
    public void addXData(XDataRecord record) { this.xData.add(record); }

    /**
     * The object's data as stored, from the type code to the end, for objects whose data this
     * library does not read (unknown classes, proxies, types the specification does not
     * describe); {@code null} for the others.
     */
    public byte[] rawData() { return rawData; }

    public void setRawData(byte[] rawData) { this.rawData = rawData; }

    /**
     * Fields kept under names (see {@link ValueHolder}); used by the readers of objects with many
     * fields. Empty for most objects.
     */
    @Override
    public Map<String, Object> values() { return values == null ? Collections.emptyMap() : Collections.unmodifiableMap(values); }

    @Override
    public void setValue(String name, Object value) {
        if (values == null) values = new LinkedHashMap<>();
        values.put(name, value);
    }
    /** Replaces each XData record by what {@code change} returns for it. */
    public void replaceXData(java.util.function.UnaryOperator<XDataRecord> change) { this.xData.replaceAll(change); }
}
