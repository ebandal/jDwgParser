package structure.entities;

import java.util.List;
import java.util.Optional;

public interface DwgObject {
    long handle();
    DwgObjectType objectType();
    int rawTypeCode();
    DwgHandleRef ownerHandle();
    List<DwgHandleRef> reactorHandles();
    Optional<DwgHandleRef> xDicHandle();
    List<XDataRecord> xData();
    boolean isEntity();
}
