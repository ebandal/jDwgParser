package io.dwg.format.r13;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitOutput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.AbstractFileStructureHandler;
import io.dwg.format.common.FileHeaderFields;
import java.util.HashMap;
import java.util.Map;

/**
 * 스펙 §3 (R13-R15 DWG FILE FORMAT ORGANIZATION) 구현
 */
public class R13FileStructureHandler extends AbstractFileStructureHandler {

    @Override
    public DwgVersion version() {
        return DwgVersion.R13;
    }

    @Override
    public boolean supports(DwgVersion version) {
        return version == DwgVersion.R13 || version == DwgVersion.R14;
    }

    @Override
    public FileHeaderFields readHeader(BitInput input) throws Exception {
        FileHeaderFields fields = new FileHeaderFields(DwgVersion.R13);

        // 버전 문자열 (6바이트)
        byte[] versionBytes = readBytes(input, 6);
        String versionStr = new String(versionBytes, java.nio.charset.StandardCharsets.US_ASCII);
        fields.setMaintenanceVersion(Integer.parseInt(versionStr.substring(4, 6)));

        // 알 수 없는 6바이트
        readBytes(input, 6);

        // 코드페이지 (2바이트)
        short codePage = input.readRawShort();
        fields.setCodePage(codePage & 0xFFFF);

        // 섹션 수 (4바이트)
        int sectionCount = input.readRawLong();

        // 섹션 Locator 배열 읽기
        java.util.List<R13SectionLocator> locators = new java.util.ArrayList<>();
        Map<String, Long> offsets = new HashMap<>();

        for (int i = 0; i < sectionCount; i++) {
            R13SectionLocator locator = R13SectionLocator.read(input);
            locators.add(locator);
            String sectionName = locator.toSectionName();
            offsets.put(sectionName, locator.seeker());
        }

        fields.setSectionOffsets(offsets);
        
        return fields;
    }

    @Override
    public Map<String, SectionInputStream> readSections(BitInput input, FileHeaderFields header) throws Exception {
        Map<String, SectionInputStream> sections = new HashMap<>();

        // TODO: Implement R13/R14 section reading
        
        return sections;
    }

    @Override
    public void writeHeader(BitOutput output, FileHeaderFields header) throws Exception {
        // 버전 문자열 (6바이트)
        String versionStr = "AC10";
        String mainVer = String.format("%02d", header.maintenanceVersion());
        writeBytes(output, (versionStr + mainVer).getBytes(java.nio.charset.StandardCharsets.US_ASCII));

        // 알 수 없는 6바이트
        writeBytes(output, new byte[6]);

        // 코드페이지 (2바이트)
        output.writeRawShort((short)header.codePage());

        // 섹션 수
        Map<String, Long> offsets = header.sectionOffsets();
        output.writeRawLong(offsets.size());

        // Locator 배열 쓰기
        for (@SuppressWarnings("unused") Map.Entry<String, Long> entry : offsets.entrySet()) {
            // Process offsets
        }
        // TODO: Implement writing locators
    }

    @Override
    public void writeSections(BitOutput output, Map<String, byte[]> sections, FileHeaderFields header) throws Exception {
        // TODO: Implement section writing for R13/R14
    }
}
