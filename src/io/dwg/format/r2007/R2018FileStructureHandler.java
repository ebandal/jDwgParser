package io.dwg.format.r2007;

import io.dwg.core.version.DwgVersion;

/**
 * 스펙 §8 (R2018) - R2013과 동일
 */
public class R2018FileStructureHandler extends R2007FileStructureHandler {

    @Override
    public DwgVersion version() {
        return DwgVersion.R2018;
    }

    @Override
    public boolean supports(DwgVersion version) {
        return version == DwgVersion.R2018;
    }
}
