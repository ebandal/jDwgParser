package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.AbstractDwgTableRecord;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * UCS (type 63), spec §20.4.62: ORIGIN, XDIR, YDIR (3BD), R2000+ ELEVATION BD, ORTHOVIEWTYPE BS,
 * ORTHOTYPE BS, kept in the record's values. Handles: xref block, R2000+ BASEUCS, NAMEDUCS.
 */
public class UcsObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.UCS.typeCode(); }

    @Override
    public boolean readsOwnHeader() { return false; }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        AbstractDwgTableRecord record = (AbstractDwgTableRecord) target;
        EntityHeaderReader.readTableRecordName(r, v, record);
        Fields.read(record, r, "ORIGIN 3BD", "XDIR 3BD", "YDIR 3BD");
        if (v.from(DwgVersion.R2000)) {
            Fields.read(record, r, "ELEVATION BD", "ORTHOVIEWTYPE BS", "ORTHOTYPE BS");
        }
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        AbstractDwgTableRecord record = (AbstractDwgTableRecord) target;
        long self = target.handle();
        record.setXrefBlockHandle(new DwgHandleRef(r.readHandleReference(self)));
        if (v.from(DwgVersion.R2000)) {
            handle(record, "BASEUCS", r, self);
            handle(record, "NAMEDUCS", r, self);
        }
    }

    private static void handle(AbstractDwgTableRecord record, String name, BitStreamReader r, long self) {
        record.setValue(name, new DwgHandleRef(r.readHandleReference(self)));
    }
}
