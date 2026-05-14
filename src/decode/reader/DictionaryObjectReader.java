package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgDictionary;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;

public class DictionaryObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.DICTIONARY.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgDictionary)) return;
        DwgDictionary dict = (DwgDictionary) target;

        int byteOff = offset;

        // Dictionary type (BS)
        if (byteOff + 2 <= data.length) {
            int type = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            dict.setDictionaryType(type);
            byteOff += 2;
        }

        // Duplicate record cloning (BS)
        if (byteOff + 2 <= data.length) {
            int cloning = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            dict.setDuplicateRecordCloning(cloning);
        }
    }
}
