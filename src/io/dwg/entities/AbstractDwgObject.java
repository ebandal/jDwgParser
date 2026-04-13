package io.dwg.entities;

import io.dwg.core.type.DwgHandleRef;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * DwgObject의 공통 필드 구현. 모든 구체 클래스의 부모.
 */
public abstract class AbstractDwgObject implements DwgObject {
    protected long handle;
    protected DwgHandleRef ownerHandle;
    protected List<DwgHandleRef> reactorHandles = new ArrayList<>();
    protected DwgHandleRef xDicHandle;
    protected List<XDataRecord> xData = new ArrayList<>();
    protected int rawTypeCode;

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
}
