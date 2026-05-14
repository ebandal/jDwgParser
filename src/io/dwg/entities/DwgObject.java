package io.dwg.entities;

import io.dwg.core.type.DwgHandleRef;
import java.util.List;
import java.util.Optional;

/**
 * 모든 DWG 객체(엔티티 + 비엔티티)의 최상위 계약.
 */
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
