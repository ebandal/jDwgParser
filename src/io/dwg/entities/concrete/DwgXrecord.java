package io.dwg.entities.concrete;

import io.dwg.core.type.DwgHandleRef;
import io.dwg.entities.AbstractDwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.XDataRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * XRECORD object (spec §20.4.104): application data as DXF group codes and values.
 */
public class DwgXrecord extends AbstractDwgObject {
    private byte[] recordData;
    private final List<XDataRecord> items = new ArrayList<>();
    private int cloning;
    private final List<DwgHandleRef> objectHandles = new ArrayList<>();

    @Override
    public DwgObjectType objectType() { return DwgObjectType.XRECORD; }

    @Override
    public boolean isEntity() { return false; }

    /** The data bytes as stored. */
    public byte[] recordData() { return recordData; }
    /**
     * The data as group codes and values, with the value types of {@link XDataRecord} and, for the
     * other DXF groups, Short, Integer, Long, Boolean (290–299) and Integer (280–289, one byte).
     * Bytes that could not be read end the list as one record with group code -1.
     */
    public List<XDataRecord> items() { return Collections.unmodifiableList(items); }
    /** R2000+: the duplicate record cloning flag (DXF 280). */
    public int duplicateRecordCloning() { return cloning; }
    /** Same as {@link #duplicateRecordCloning()}. */
    public int recordType() { return cloning; }
    /** The object handles at the end of the handle stream. */
    public List<DwgHandleRef> objectHandles() { return Collections.unmodifiableList(objectHandles); }

    public void setRecordData(byte[] data) { this.recordData = data; }
    public void addItem(XDataRecord item) { items.add(item); }
    public void setDuplicateRecordCloning(int cloning) { this.cloning = cloning; }
    public void addObjectHandle(DwgHandleRef handle) { objectHandles.add(handle); }
}
