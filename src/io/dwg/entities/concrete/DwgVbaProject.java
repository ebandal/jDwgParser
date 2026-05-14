package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgObject;
import io.dwg.entities.DwgObjectType;

/**
 * VBA_PROJECT 오브젝트 (타입 0x4F)
 * VBA 프로젝트 정보
 */
public class DwgVbaProject extends AbstractDwgObject {
    @Override
    public DwgObjectType objectType() { return DwgObjectType.VBA_PROJECT; }

    @Override
    public boolean isEntity() { return false; }
}
