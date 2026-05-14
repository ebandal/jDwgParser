package io.dwg.sections.objects.writers;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgDictionary;
import io.dwg.sections.objects.ObjectWriter;
import java.util.Map;

public class DictionaryObjectWriter implements ObjectWriter {
    @Override
    public int objectType() { return DwgObjectType.DICTIONARY.typeCode(); }

    @Override
    public void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception {
        DwgDictionary dict = (DwgDictionary) source;
        w.writeBitShort(dict.dictionaryType());
        w.writeBitShort(dict.duplicateRecordCloning());
        Map<String, Object> entries = dict.entries();
        w.writeBitLong(entries.size());
        for (Map.Entry<String, Object> entry : entries.entrySet()) {
            w.writeVariableText(entry.getKey());
            w.writeBitLongLong((Long) entry.getValue());
        }
    }
}
