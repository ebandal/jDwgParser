package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgDictionary;
import io.dwg.sections.objects.ObjectReader;

/**
 * DICTIONARY (type 42), spec §20.4.44.
 * <pre>
 *   number of items BL, R14 an unknown RC, R2000+ cloning flag BS and hard owner flag RC,
 *   the item names TV
 *   handles: (parent, reactors, xdictionary,) one item handle per name
 * </pre>
 */
public class DictionaryObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.DICTIONARY.typeCode(); }

    @Override
    public boolean readsOwnHeader() { return false; }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgDictionary dict = (DwgDictionary) target;
        int count = r.readBitLong();
        if (v.from(DwgVersion.R14) && !v.from(DwgVersion.R2000)) {
            r.readRawChar();                       // unknown, always 0
        }
        if (v.from(DwgVersion.R2000)) {
            dict.setDuplicateRecordCloning(r.readBitShort());
            dict.setHardOwner(r.readRawChar() != 0);
        }
        for (int i = 0; i < count; i++) {
            dict.addName(r.readVariableText());
        }
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgDictionary dict = (DwgDictionary) target;
        for (String name : dict.names()) {
            dict.addEntry(name, r.readRef(target.handle()));
        }
    }
}
