package io.dwg.sections;

import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;

/**
 * 모든 섹션 파서의 계약.
 */
public interface SectionParser<T> {
    T parse(SectionInputStream stream, DwgVersion version) throws Exception;
    String sectionName();
    boolean supports(DwgVersion version);
}
