package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgPlaceholder;
import io.dwg.sections.objects.ObjectReader;

/**
 * PLACEHOLDER 오브젝트 리더 (타입 0x4E)
 * 예약됨 placeholder 객체
 */
public class PlaceholderReader implements ObjectReader {
    @Override
    public int objectType() { return DwgObjectType.PLACEHOLDER.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        DwgPlaceholder placeholder = (DwgPlaceholder) target;
        // Placeholder has no additional data
    }
}
