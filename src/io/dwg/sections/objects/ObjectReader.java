package io.dwg.sections.objects;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;

/**
 * 개별 객체 타입의 추가 데이터 파싱 계약.
 */
public interface ObjectReader {
    void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception;
    int objectType();
}
