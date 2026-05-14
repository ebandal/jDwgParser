package io.dwg.format.r13;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitOutput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.AbstractFileStructureHandler;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.format.common.SectionType;
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

                // R13 섹션 구조:
                // [Section Start Sentinel: 16 bytes]
                // [Actual section data]
                // [Section End Sentinel: 16 bytes]
                // [CRC: 2 bytes]
                // 총 크기 = 16 + data + 16 + 2

                // 시작 sentinel 읽기 (16 바이트)
                byte[] startSentinel = new byte[16];
                for (int i = 0; i < 16; i++) {
                    startSentinel[i] = (byte) input.readRawChar();
                }
                System.out.printf("[DEBUG] R13: Section '%s' start sentinel: %s\n",
                    sectionName, formatHex(startSentinel, 0, Math.min(16, startSentinel.length)));

                // 실제 데이터 크기 = 전체 크기 - sentinel(16) - sentinel(16) - CRC(2)
                long dataSize = size - 34;
                if (dataSize < 0) {
                    System.err.printf("[DEBUG] R13: Invalid section size for '%s': %d\n", sectionName, size);
                    continue;
                }

                // 섹션 데이터를 읽음
                byte[] sectionData = new byte[(int) dataSize];
                for (int i = 0; i < dataSize; i++) {
                    sectionData[i] = (byte) input.readRawChar();
                }

                // 끝 sentinel 읽기 (16 바이트)
                byte[] endSentinel = new byte[16];
                for (int i = 0; i < 16; i++) {
                    endSentinel[i] = (byte) input.readRawChar();
                }
                System.out.printf("[DEBUG] R13: Section '%s' end sentinel: %s\n",
                    sectionName, formatHex(endSentinel, 0, Math.min(16, endSentinel.length)));

                // CRC 읽기 (2 바이트)
                short crc = input.readRawShort();
                System.out.printf("[DEBUG] R13: Section '%s' CRC: 0x%04X\n", sectionName, crc & 0xFFFF);

                sections.put(sectionName, new SectionInputStream(sectionData, sectionName));
                System.out.printf("[DEBUG] R13: Section '%s' read: %d bytes of actual data\n", sectionName, sectionData.length);

            } catch (Exception e) {
                System.err.printf("[DEBUG] R13: Failed to read section '%s': %s\n", sectionName, e.getMessage());
                e.printStackTrace();
            }
        }

        return sections;
    }

    /**
     * 바이트 배열을 16진수 문자열로 변환 (디버그용)
     */
    private String formatHex(byte[] data, int offset, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < offset + length && i < data.length; i++) {
            sb.append(String.format("%02X ", data[i] & 0xFF));
        }
        return sb.toString();
    }

    @Override
    public void writeHeader(BitOutput output, FileHeaderFields header) throws Exception {
        System.out.println("[DEBUG] R13.writeHeader: start");

        // 1. Version string (6 bytes) - "AC1012" (R13) or "AC1014" (R14)
        String versionStr = (header.version() == DwgVersion.R14) ? "AC1014" : "AC1012";
        byte[] versionBytes = versionStr.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        for (byte b : versionBytes) {
            output.writeRawChar(b & 0xFF);
        }

        // 2. Reserved (6 bytes)
        for (int i = 0; i < 6; i++) {
            output.writeRawChar(0);
        }

        // 3. RC (1 byte): zero_one_or_three
        output.writeRawChar(0);

        // 4. RL (4 bytes): thumbnail_address
        output.writeRawLong(0);

        // 5. RC (1 byte): dwg_version
        output.writeRawChar(0);

        // 6. RC (1 byte): maint_version
        output.writeRawChar(0);

        // 7. RS (2 bytes): codepage
        output.writeRawShort((short) header.codePage());

        // 8. RC (1 byte): section count
        var locators = header.sectionLocators();
        int sectionCount = locators != null ? locators.size() : 0;
        output.writeRawChar(sectionCount);

        // 9. Section locator array
        if (locators != null) {
            for (Object locObj : locators) {
                if (locObj instanceof R13SectionLocator loc) {
                    loc.write(output);
                }
            }
        }

        // 10. RS (2 bytes): CRC
        output.writeRawShort((short) 0);

        System.out.println("[DEBUG] R13.writeHeader: complete, " + sectionCount + " sections");
    }

    @Override
    public void writeSections(BitOutput output, Map<String, byte[]> sections, FileHeaderFields header) throws Exception {
        System.out.println("[DEBUG] R13.writeSections: start");

        // Section order matches locator record numbers: 0=HEADER, 1=CLASSES, 2=HANDLES, 3=OBJECTS
        String[] sectionOrder = {
            SectionType.HEADER.sectionName(),
            SectionType.CLASSES.sectionName(),
            SectionType.HANDLES.sectionName(),
            SectionType.OBJECTS.sectionName()
        };

        for (String sectionName : sectionOrder) {
            byte[] data = sections.get(sectionName);
            if (data == null) data = new byte[0];

            // Write 16-byte start sentinel (zeros)
            for (int i = 0; i < 16; i++) {
                output.writeRawChar(0);
            }

            // Write actual data
            for (byte b : data) {
                output.writeRawChar(b & 0xFF);
            }

            // Write 16-byte end sentinel (zeros)
            for (int i = 0; i < 16; i++) {
                output.writeRawChar(0);
            }

            // Write 2-byte CRC (0)
            output.writeRawShort((short) 0);

            System.out.println("[DEBUG] R13: Section '" + sectionName + "' written (" + data.length + " bytes)");
        }

        System.out.println("[DEBUG] R13.writeSections: complete");
    }
}
