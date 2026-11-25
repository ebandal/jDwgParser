package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * For types the specification does not describe (VISUALSTYLE, MATERIAL, PERSSUBENTMANAGER,
 * SURFACE, MESH, UNDERLAY): only the common object or entity data is read; the object keeps its
 * bytes. The library does not guess the layout of the rest.
 */
public class UndocumentedObjectReader implements ObjectReader {

    private final DwgObjectType type;
    private final boolean entity;

    public UndocumentedObjectReader(DwgObjectType type, boolean entity) {
        this.type = type;
        this.entity = entity;
    }

    @Override
    public int objectType() { return type.typeCode(); }

    @Override
    public boolean readsOwnHeader() { return entity; }

    @Override
    public boolean readsAllData() { return false; }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        if (entity) {
            EntityHeaderReader.readEntityHeader(r, v);
            EntityHeaderReader.readCommonEntityData(r, v);
        }
    }
}
