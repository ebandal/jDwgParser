package io.dwg.entities;

import io.dwg.core.type.CmColor;
import io.dwg.core.type.DwgHandleRef;

/**
 * 도면에 실제로 그려지는 엔티티의 추가 계약.
 */
public interface DwgEntity extends DwgObject {
    DwgHandleRef layerHandle();
    DwgHandleRef lineTypeHandle();
    double lineTypeScale();
    CmColor color();
    int invisibility();
    int entityMode();
    double lineWeight();
}
