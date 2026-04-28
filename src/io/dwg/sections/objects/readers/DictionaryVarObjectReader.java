package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgDictionaryVar;
import io.dwg.sections.objects.ObjectReader;

public class DictionaryVarObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.ACAD_DICTIONARYVAR.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgDictionaryVar var = (DwgDictionaryVar) target;

        // Variable name
        var.setVarName(r.readText());

        // Variable type
        var.setVarType(r.readBitShort());

        // Variable value
        var.setVarValue(r.readText());
    }
}
