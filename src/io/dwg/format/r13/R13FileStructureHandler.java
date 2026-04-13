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

        // R13 헤더 구조 (libredwg header.spec 참고)
        // 0x00-0x05: Version string (6 바이트)
        // 0x06-0x0B: Reserved (6 바이트)
        // 0x0C 이후: Header fields 시작

        // 1. Version string (6 바이트): AC1012, AC1013, 또는 AC1014
        byte[] versionBytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            versionBytes[i] = (byte) input.readRawChar();
        }
        String version = new String(versionBytes, java.nio.charset.StandardCharsets.US_ASCII);
        if (!version.startsWith("AC101")) {
            throw new IllegalArgumentException("Invalid R13 version string: " + version);
        }

        // 2. Reserved (6 바이트)
        for (int i = 0; i < 6; i++) {
            input.readRawChar();
        }

        // 3. RC (1 바이트): zero_one_or_three
        input.readRawChar();

        // 4. RL (4 바이트): thumbnail_address
        int thumbnailAddress = input.readRawLong();
        fields.setPreviewOffset(thumbnailAddress & 0xFFFFFFFFL);

        // 5. RC (1 바이트): dwg_version
        input.readRawChar();

        // 6. RC (1 바이트): maint_version
        int maintVersion = input.readRawChar();
        fields.setMaintenanceVersion(maintVersion);

        // 7. RS (2 바이트): codepage
        short codePage = input.readRawShort();
        fields.setCodePage(codePage & 0xFFFF);

        // 8. RC (1 바이트): sections (섹션 개수)
        int sectionCount = input.readRawChar();
        System.out.printf("[DEBUG] R13: Found %d sections\n", sectionCount);

        // 9. 섹션 Locator 배열 읽기
        java.util.List<R13SectionLocator> locators = new java.util.ArrayList<>();
        Map<String, Long> offsets = new HashMap<>();
        Map<String, Long> sizes = new HashMap<>();

        for (int i = 0; i < sectionCount; i++) {
            R13SectionLocator locator = R13SectionLocator.read(input);
            locators.add(locator);
            String sectionName = locator.toSectionName();
            offsets.put(sectionName, locator.seeker());
            sizes.put(sectionName, locator.size());
            System.out.printf("[DEBUG] R13 Section %d: name='%s', offset=0x%X, size=0x%X\n",
                i, sectionName, locator.seeker(), locator.size());
        }

        fields.setSectionOffsets(offsets);
        fields.setSectionSizes(sizes);

        // 10. RS (2 바이트): CRC 검증 (섹션 locators 이후)
        short crc = input.readRawShort();
        System.out.printf("[DEBUG] R13 Header CRC: 0x%04X\n", crc & 0xFFFF);

        return fields;
    }

    @Override
    public Map<String, SectionInputStream> readSections(BitInput input, FileHeaderFields header) throws Exception {
        Map<String, SectionInputStream> sections = new HashMap<>();

        // R13/R14에서 섹션 위치는 헤더에 저장됨
        if (header.sectionOffsets() == null || header.sectionOffsets().isEmpty()) {
            System.err.println("[DEBUG] R13: No section offsets found in header");
            return sections;
        }

        if (header.sectionSizes() == null) {
            System.err.println("[DEBUG] R13: No section sizes found in header");
            return sections;
        }

        System.out.printf("[DEBUG] R13: Reading %d sections\n", header.sectionOffsets().size());

        // 각 섹션을 읽음
        for (String sectionName : header.sectionOffsets().keySet()) {
            try {
                long offset = header.sectionOffsets().get(sectionName);
                long size = header.sectionSizes().get(sectionName);

                System.out.printf("[DEBUG] R13: Reading section '%s' at offset 0x%X, size=0x%X\n",
                    sectionName, offset, size);

                // 파일의 offset 위치로 이동 (바이트 단위를 비트 단위로 변환)
                input.seek(offset * 8);

                // 섹션 데이터를 읽음 (지정된 크기만큼)
                byte[] sectionData = new byte[(int) size];
                for (int i = 0; i < size; i++) {
                    sectionData[i] = (byte) input.readRawChar();
                }

                sections.put(sectionName, new SectionInputStream(sectionData, sectionName));
                System.out.printf("[DEBUG] R13: Section '%s' read: %d bytes\n", sectionName, sectionData.length);

            } catch (Exception e) {
                System.err.printf("[DEBUG] R13: Failed to read section '%s': %s\n", sectionName, e.getMessage());
            }
        }

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
