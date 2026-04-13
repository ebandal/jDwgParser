package io.dwg.format.r2007;

import io.dwg.core.version.DwgVersion;

/**
 * 스펙 §7 (R2013) - R2007 구조와 동일
 */
public class R2013FileStructureHandler extends R2007FileStructureHandler {

    @Override
    public DwgVersion version() {
        return DwgVersion.R2013;
    }

    @Override
    public boolean supports(DwgVersion version) {
        return version == DwgVersion.R2013;
    }
}
