package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgAppId;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * APPID (type 67), spec §20.4.66.
 * <pre>
 *   name TV, table record flags, unknown RC (DXF 71)
 *   handles: (control, reactors, xdictionary,) xref block
 * </pre>
 */
public class AppIdObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.APPID.typeCode(); }

    @Override
    public boolean readsOwnHeader() { return false; }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgAppId appId = (DwgAppId) target;
        EntityHeaderReader.readTableRecordName(r, v, appId);
        appId.setFlags(r.readRawChar());
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        ((DwgAppId) target).setXrefBlockHandle(new DwgHandleRef(r.readHandleReference(target.handle())));
    }
}
