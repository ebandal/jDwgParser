package io.dwg.format.common;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitOutput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;
import java.util.Map;

/**
 * 버전별 파일 구조 처리기의 계약 (Strategy 패턴)
 */
public interface DwgFileStructureHandler {
    
    /**
     * 이 핸들러가 담당하는 DWG 버전 반환
     */
    DwgVersion version();

    /**
     * 파일 헤더 파싱 후 공통 헤더 필드 반환
     */
    FileHeaderFields readHeader(BitInput input) throws Exception;

    /**
     * 섹션명 → 원시 바이트 맵 구성
     */
    Map<String, SectionInputStream> readSections(BitInput input, FileHeaderFields header) throws Exception;

    /**
     * 헤더를 파일에 씀
     */
    void writeHeader(BitOutput output, FileHeaderFields header) throws Exception;

    /**
     * 각 섹션 바이트를 파일 구조에 맞게 씀
     */
    void writeSections(BitOutput output, Map<String, byte[]> sections, FileHeaderFields header) throws Exception;

    /**
     * 이 핸들러가 해당 버전을 처리할 수 있는지
     */
    boolean supports(DwgVersion version);
}
