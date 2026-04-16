package io.dwg.format.r2010;

import io.dwg.core.version.DwgVersion;
import io.dwg.format.r2007.R2007FileStructureHandler;

/**
 * 스펙 §6 (R2010) - R2007 구조와 동일
 * R2010은 R2007 포맷 확장으로, 동일한 파일 구조 사용
 */
public class R2010FileStructureHandler extends R2007FileStructureHandler {

    @Override
    public DwgVersion version() {
        return DwgVersion.R2010;
    }

    @Override
    public boolean supports(DwgVersion version) {
        return version == DwgVersion.R2010;
    }
}
