package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgDictionary;
import io.dwg.sections.objects.ObjectReader;

/**
 * DICTIONARY 엔티티 리더 (타입 0x2A)
 * 이름-값 쌍의 집합
 */
public class DictionaryObjectReader implements ObjectReader {
    @Override
    public int objectType() { return DwgObjectType.DICTIONARY.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgDictionary dict = (DwgDictionary) target;

        // 1. dictionaryType (BS)
        int dicType = r.readBitShort();
        dict.setDictionaryType(dicType);

        // 2. duplicateRecordCloning (BS)
        int dupCloning = r.readBitShort();
        dict.setDuplicateRecordCloning(dupCloning);

        // 3. numEntries (BL)
        long beforeNumEntries = r.position();
        long numEntries = r.readBitLong();

        // Safety check for invalid numEntries
        if (numEntries < 0 || numEntries > 100000) {
            System.err.printf("[WARN] DictionaryObjectReader: numEntries=%d at bit offset %d, skipping entries%n",
                numEntries, beforeNumEntries);
            return;
        }

        // 4. entries - numEntries개의 (name, handle) 쌍
        for (int i = 0; i < numEntries; i++) {
            String name = r.readVariableText();
            // Handle would be resolved in post-processing
            long handle = r.readBitLongLong();
            dict.addEntry(name, handle);
        }
    }
}
