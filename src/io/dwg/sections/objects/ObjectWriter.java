package io.dwg.sections.objects;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;

/**
 * 개별 객체 타입 직렬화 계약.
 */
public interface ObjectWriter {
    void write(DwgObject source, BitStreamWriter w, DwgVersion v) throws Exception;
    int objectType();
}
