package decode.reader;

import structure.DwgVersion;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;

public class BlockEndObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.BLOCK_END.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        // BlockEnd has no fields to read - it's just a marker
    }
}
