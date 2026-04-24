package io.dwg.entities.concrete;

import io.dwg.entities.AbstractDwgObject;
import io.dwg.entities.DwgObjectType;

/**
 * PLACEHOLDER 오브젝트 (타입 0x4E)
 * 예약됨 placeholder 객체
 */
public class DwgPlaceholder extends AbstractDwgObject {
    @Override
    public DwgObjectType objectType() { return DwgObjectType.PLACEHOLDER; }

    @Override
    public boolean isEntity() { return false; }
}
