package decode.reader;

import structure.DwgVersion;
import structure.entities.DwgObject;

public interface ObjectReader {
    void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception;
    int objectTypeCode();
}
