package io.dwg.format.r2000;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitOutput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.AbstractFileStructureHandler;
import io.dwg.format.common.FileHeaderFields;
import java.util.HashMap;
import java.util.Map;

/**
 * R2000 DWG FILE FORMAT ORGANIZATION (스펙 §3 변형)
 * R13/R14와 유사한 구조이지만 몇 가지 확장 포함
 */
public class R2000FileStructureHandler extends AbstractFileStructureHandler {

    @Override
    public DwgVersion version() {
        return DwgVersion.R2000;
    }

    @Override
    public boolean supports(DwgVersion version) {
        return version == DwgVersion.R2000;
    }

    @Override
    public FileHeaderFields readHeader(BitInput input) throws Exception {
        FileHeaderFields fields = new FileHeaderFields(DwgVersion.R2000);

        // R2000 헤더 구조 (libredwg 참고)
        // 0x00-0x05: Version string (6 바이트) = "AC1015"
        // 0x06-0x0B: Reserved (6 바이트)
        // 0x0C 이후: Header fields

        // 1. Version string (6 바이트): AC1015
        byte[] versionBytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            versionBytes[i] = (byte) input.readRawChar();
        }
        String version = new String(versionBytes, java.nio.charset.StandardCharsets.US_ASCII);
        if (!version.equals("AC1015")) {
            throw new IllegalArgumentException("Invalid R2000 version string: " + version);
        }

        // 2. Reserved (6 바이트)
        for (int i = 0; i < 6; i++) {
            input.readRawChar();
        }

        // 3. RC (1 바이트): unknown_0
        input.readRawChar();

        // 4. RL (4 바이트): preview address
        int previewAddr = input.readRawLong();
        fields.setPreviewOffset(previewAddr & 0xFFFFFFFFL);

        // 5. RC (1 바이트): Dwg version (should be 0)
        input.readRawChar();

        // 6. RC (1 바이트): Maintenance version
        int maintVersion = input.readRawChar();
        fields.setMaintenanceVersion(maintVersion);

        // 7. RS (2 바이트): Codepage
        short codePage = input.readRawShort();
        fields.setCodePage(codePage & 0xFFFF);

        // 8. RC (1 바이트): Number of sections
        int sectionCount = input.readRawChar();
        System.out.printf("[DEBUG] R2000: Found %d sections\n", sectionCount);

        // 9. 섹션 Locator 배열 읽기
        // R2000: 섹션이 정해진 순서로 나타남 (0=Header, 1=Classes, 2=Handles, 3=Objects, ...)
        Map<String, Long> offsets = new HashMap<>();
        Map<String, Long> sizes = new HashMap<>();

        String[] sectionNames = {
            io.dwg.format.common.SectionType.HEADER.sectionName(),
            io.dwg.format.common.SectionType.CLASSES.sectionName(),
            io.dwg.format.common.SectionType.HANDLES.sectionName(),
            io.dwg.format.common.SectionType.OBJECTS.sectionName(),
            "AcDb:AuxHeader",
            "AcDb:AppInfoHeader"
        };

        for (int i = 0; i < sectionCount && i < sectionNames.length; i++) {
            R2000SectionLocator locator = R2000SectionLocator.read(input);
            // R2000에서는 recordNumber를 무시하고 순서대로 처리
            String sectionName = sectionNames[i];
            offsets.put(sectionName, locator.seeker());
            sizes.put(sectionName, locator.size());
            System.out.printf("[DEBUG] R2000 Section %d: name='%s', offset=0x%X, size=0x%X\n",
                i, sectionName, locator.seeker(), locator.size());
        }

        fields.setSectionOffsets(offsets);
        fields.setSectionSizes(sizes);

        // 10. RS (2 바이트): CRC
        short crc = input.readRawShort();
        System.out.printf("[DEBUG] R2000 Header CRC: 0x%04X\n", crc & 0xFFFF);

        return fields;
    }

    @Override
    public Map<String, SectionInputStream> readSections(BitInput input, FileHeaderFields header) throws Exception {
        Map<String, SectionInputStream> sections = new HashMap<>();
        Map<String, Long> offsets = header.sectionOffsets();
        Map<String, Long> sizes = header.sectionSizes();

        // DEBUG: Print actual sizes before processing
        System.out.println("[DEBUG] Section sizes from header:");
        for (String name : offsets.keySet()) {
            long size = sizes.get(name);
            System.out.printf("  %s: 0x%X (%d bytes)\n", name, size, size);
        }

        for (String sectionName : offsets.keySet()) {
            long offset = offsets.get(sectionName);
            long size = sizes.get(sectionName);

            // Skip obviously invalid sections
            if (size > 0xF0000000L) {
                System.out.printf("[WARN] Skipping %s: size too large (0x%X)\n", sectionName, size);
                continue;
            }

            if (offset > 0 && size > 0) {
                byte[] sectionData = new byte[(int) size];
                input.seek(offset * 8); // Convert byte offset to bit offset
                for (int i = 0; i < size; i++) {
                    sectionData[i] = (byte) input.readRawChar();
                }
                sections.put(sectionName, new SectionInputStream(sectionData, sectionName));
            }
        }

        return sections;
    }

    @Override
    public void writeHeader(BitOutput output, FileHeaderFields header) throws Exception {
        // R2000 version string: AC1015
        String versionStr = "AC1015";
        for (byte b : versionStr.getBytes(java.nio.charset.StandardCharsets.US_ASCII)) {
            output.writeRawChar(b & 0xFF);
        }

        // Reserved 6 bytes
        for (int i = 0; i < 6; i++) {
            output.writeRawChar(0);
        }

        // unknown_0 (RC)
        output.writeRawChar(0);

        // preview address (RL)
        output.writeRawLong((int) header.previewOffset());

        // dwg_version (RC)
        output.writeRawChar(0);

        // maint_version (RC)
        output.writeRawChar(header.maintenanceVersion());

        // codepage (RS)
        output.writeRawShort((short) header.codePage());

        // Section count (RC)
        int sectionCount = 0;
        String[] sectionNames = {
            io.dwg.format.common.SectionType.HEADER.sectionName(),
            io.dwg.format.common.SectionType.CLASSES.sectionName(),
            io.dwg.format.common.SectionType.HANDLES.sectionName(),
            io.dwg.format.common.SectionType.OBJECTS.sectionName()
        };

        // Determine section count (default to all 4 if offsets not set)
        java.util.Map<String, Long> offsets = header.sectionOffsets();
        if (offsets != null && !offsets.isEmpty()) {
            for (String name : sectionNames) {
                if (offsets.containsKey(name)) {
                    sectionCount++;
                }
            }
        } else {
            // Default: all standard sections
            sectionCount = sectionNames.length;
        }
        output.writeRawChar(sectionCount);

        // Write section locators
        if (header.sectionLocators() != null) {
            for (io.dwg.format.r13.R13SectionLocator loc : header.sectionLocators()) {
                loc.write(output);
            }
        }

        // CRC (RS) - placeholder
        output.writeRawShort((short) 0);
    }

    @Override
    public void writeSections(BitOutput output, Map<String, byte[]> sections, FileHeaderFields header) throws Exception {
        // R2000 section layout: 16B sentinel + data + 16B sentinel + 2B CRC (same as R13)
        String[] sectionOrder = {
            io.dwg.format.common.SectionType.HEADER.sectionName(),
            io.dwg.format.common.SectionType.CLASSES.sectionName(),
            io.dwg.format.common.SectionType.HANDLES.sectionName(),
            io.dwg.format.common.SectionType.OBJECTS.sectionName()
        };

        for (String sectionName : sectionOrder) {
            byte[] data = sections.get(sectionName);
            if (data == null) data = new byte[0];

            // Write 16-byte start sentinel (zeros)
            for (int i = 0; i < 16; i++) {
                output.writeRawChar(0);
            }

            // Write section data
            for (byte b : data) {
                output.writeRawChar(b & 0xFF);
            }

            // Write 16-byte end sentinel (zeros)
            for (int i = 0; i < 16; i++) {
                output.writeRawChar(0);
            }

            // Write 2-byte CRC (placeholder)
            output.writeRawShort((short) 0);
        }
    }
}
