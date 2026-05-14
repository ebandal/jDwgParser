package io.dwg.sections;

import io.dwg.core.io.SectionOutputStream;
import io.dwg.core.version.DwgVersion;

/**
 * 모든 섹션 라이터의 계약.
 */
public interface SectionWriter<T> {
    SectionOutputStream write(T data, DwgVersion version) throws Exception;
    String sectionName();
}
