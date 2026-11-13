package io.dwg.entities;

import io.dwg.core.type.DwgHandleRef;

/**
 * 모든 DIMENSION 엔티티의 공통 부분.
 * 치수의 그림은 익명 블록에 들어 있고, 그 블록의 BLOCK_HEADER를 {@link #blockHandle()}이 가리킨다.
 */
public interface DwgDimension extends DwgEntity {
    DwgHandleRef dimStyleHandle();
    DwgHandleRef blockHandle();
    void setDimStyleHandle(DwgHandleRef dimStyleHandle);
    void setBlockHandle(DwgHandleRef blockHandle);
}
