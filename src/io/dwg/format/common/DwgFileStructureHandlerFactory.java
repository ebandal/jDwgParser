package io.dwg.format.common;

import io.dwg.core.version.DwgVersion;
import io.dwg.core.exception.DwgVersionException;

/**
 * DwgVersion에 맞는 DwgFileStructureHandler 인스턴스를 반환하는 팩토리
 */
public class DwgFileStructureHandlerFactory {

    /**
     * version switch → 적합한 Handler 인스턴스 반환
     */
    public static DwgFileStructureHandler forVersion(DwgVersion version) {
        switch (version) {
            case R13:
            case R14:
                return new io.dwg.format.r13.R13FileStructureHandler();
            case R2000:
                return new io.dwg.format.r2000.R2000FileStructureHandler();
            case R2004:
            case R2010:
            case R2013:
            case R2018:
                // libredwg's decode.c (line 222-226) uses decode_R2004 for R2010+ files.
                // R2010, R2013, R2018 share R2004 file structure (only version string differs).
                return new io.dwg.format.r2004.R2004FileStructureHandler();
            case R2007:
                // R2007 uses its own RS-encoded format (the only version using RS).
                return new io.dwg.format.r2007.R2007FileStructureHandler();
            default:
                throw new DwgVersionException("Unsupported version: " + version);
        }
    }

    /**
     * 파일 헤더에서 버전 자동 감지 후 위임
     */
    public static DwgFileStructureHandler detect(byte[] fileBytes) {
        DwgVersion version = io.dwg.core.version.DwgVersionDetector.detect(fileBytes);
        return forVersion(version);
    }
}
