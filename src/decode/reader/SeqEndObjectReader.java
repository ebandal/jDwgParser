package decode.reader;

import structure.DwgVersion;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;

public class SeqEndObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.SEQEND.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        // SeqEnd has no fields to read - it's just a marker
    }
}
