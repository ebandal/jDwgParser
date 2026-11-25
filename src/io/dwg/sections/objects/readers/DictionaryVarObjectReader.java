package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgDictionaryVar;
import io.dwg.sections.objects.ObjectReader;

/**
 * DICTIONARYVAR (a class), spec §20.4.74: an integer RC and a string. The specification gives
 * the string's type as BS; it is a TV. The variable's name is the key of the dictionary entry
 * that points at it.
 */
public class DictionaryVarObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.ACAD_DICTIONARYVAR.typeCode(); }

    @Override
    public boolean readsOwnHeader() { return false; }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgDictionaryVar var = (DwgDictionaryVar) target;
        var.setVarType(r.readRawChar());
        var.setVarValue(r.readVariableText());
    }
}
