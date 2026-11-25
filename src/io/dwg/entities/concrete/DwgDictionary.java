package io.dwg.entities.concrete;

import io.dwg.core.type.DwgHandleRef;
import io.dwg.entities.AbstractDwgObject;
import io.dwg.entities.DwgObjectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DICTIONARY object (type 42, spec §20.4.44): named entries that point at other objects. The
 * named object dictionary and the extension dictionaries of objects are DICTIONARYs.
 */
public class DwgDictionary extends AbstractDwgObject {
    private int cloning;
    private boolean hardOwner;
    private final List<String> names = new ArrayList<>();
    private final Map<String, DwgHandleRef> entries = new LinkedHashMap<>();

    @Override
    public DwgObjectType objectType() { return DwgObjectType.DICTIONARY; }

    @Override
    public boolean isEntity() { return false; }

    /** R2000+: the duplicate record cloning flag (DXF 281). */
    public int duplicateRecordCloning() { return cloning; }
    /** Same as {@link #duplicateRecordCloning()}. */
    public int dictionaryType() { return cloning; }
    /** R2000+: the dictionary owns its entries (DXF 280). */
    public boolean isHardOwner() { return hardOwner; }
    /** The entries in file order: name and the handle of the object. */
    public Map<String, DwgHandleRef> entries() { return Collections.unmodifiableMap(entries); }
    public DwgHandleRef getEntry(String name) { return entries.get(name); }
    /** The entry names as read from the data, before their handles are known. */
    public List<String> names() { return Collections.unmodifiableList(names); }

    public void setDuplicateRecordCloning(int cloning) { this.cloning = cloning; }
    public void setHardOwner(boolean hardOwner) { this.hardOwner = hardOwner; }
    public void addName(String name) { names.add(name); }
    public void addEntry(String name, DwgHandleRef handle) { entries.put(name, handle); }
}
