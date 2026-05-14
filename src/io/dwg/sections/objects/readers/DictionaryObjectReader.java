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
        io.dwg.entities.AbstractDwgObject ao = (io.dwg.entities.AbstractDwgObject) target;

        // DICTIONARY doesn't have standard common header, so read it here
        // 1. numReactors (BL)
        long numReactors = r.readBitLong();
        if (numReactors < 0 || numReactors > 100000) {
            numReactors = 0;
        }
        for (int i = 0; i < numReactors; i++) {
            ao.addReactorHandle(new io.dwg.core.type.DwgHandleRef(r.readHandle()));
        }

        // 2. isXDic (B) - R2004+
        if (v.from(io.dwg.core.version.DwgVersion.R2004)) {
            boolean hasXDic = r.getInput().readBit();
            if (hasXDic) {
                ao.setXDicHandle(new io.dwg.core.type.DwgHandleRef(r.readHandle()));
            }
        }

        // 3. owner handle (H)
        ao.setOwnerHandle(new io.dwg.core.type.DwgHandleRef(r.readHandle()));

        // 4. dictionaryType (BS)
        int dicType = r.readBitShort();
        dict.setDictionaryType(dicType);

        // 5. duplicateRecordCloning (BS)
        int dupCloning = r.readBitShort();
        dict.setDuplicateRecordCloning(dupCloning);

        // 6. numEntries (BL)
        try {
            long numEntries = r.readBitLong();

            // Safety check for invalid numEntries
            if (numEntries < 0 || numEntries > 100000) {
                System.err.printf("[WARN] DictionaryObjectReader: numEntries=%d (invalid), skipping entries%n",
                    numEntries);
                return;
            }

            // 4. entries - numEntries개의 (name, handle) 쌍
            for (int i = 0; i < numEntries; i++) {
                String name = r.readVariableText();
                // Handle would be resolved in post-processing
                long handle = r.readBitLongLong();
                dict.addEntry(name, handle);
            }
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("Invalid BL opcode")) {
                // Bit stream is misaligned or corrupted - log and skip
                System.err.printf("[WARN] DictionaryObjectReader: %s, skipping dictionary%n", e.getMessage());
            } else {
                throw e;
            }
        }
    }
}
