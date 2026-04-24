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

        // R2000 헤더 구조 (libredwg decode.c 참고)
        // 0x00-0x05: Version string (6 바이트) = "AC1015"
        // 0x06-0x0B: Reserved (6 바이트)
        // 0x0C 이후: Header fields

        System.out.printf("[DEBUG] R2000 Header: start position bits=%d\n", input.position());

        // 1. Version string (6 바이트): AC1015
        byte[] versionBytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            versionBytes[i] = (byte) input.readRawChar();
        }
        System.out.printf("[DEBUG] R2000 Header: after version bits=%d (byte=%d)\n", input.position(), input.position()/8);
        String version = new String(versionBytes, java.nio.charset.StandardCharsets.US_ASCII);
        if (!version.equals("AC1015")) {
            throw new IllegalArgumentException("Invalid R2000 version string: " + version);
        }

        // 2. Reserved (6 바이트)
        for (int i = 0; i < 6; i++) {
            input.readRawChar();
        }
        System.out.printf("[DEBUG] R2000 Header: after reserved bits=%d (byte=%d)\n", input.position(), input.position()/8);

        // 3. RC (1 바이트): unknown_0
        input.readRawChar();
        System.out.printf("[DEBUG] R2000 Header: after unknown_0 bits=%d (byte=%d)\n", input.position(), input.position()/8);

        // 4. RL (4 바이트): preview address
        int previewAddr = input.readRawLong();
        System.out.printf("[DEBUG] R2000 Header: after previewAddr bits=%d (byte=%d), previewAddr=0x%X\n", input.position(), input.position()/8, previewAddr);
        fields.setPreviewOffset(previewAddr & 0xFFFFFFFFL);

        // 5. RC (1 바이트): Dwg version (should be 0)
        input.readRawChar();
        System.out.printf("[DEBUG] R2000 Header: after dwgVersion bits=%d (byte=%d)\n", input.position(), input.position()/8);

        // 6. RC (1 바이트): Maintenance version
        int maintVersion = input.readRawChar();
        fields.setMaintenanceVersion(maintVersion);
        System.out.printf("[DEBUG] R2000 Header: after maintVersion bits=%d (byte=%d)\n", input.position(), input.position()/8);

        // 7. RS (2 바이트): Codepage
        short codePage = input.readRawShort();
        fields.setCodePage(codePage & 0xFFFF);
        System.out.printf("[DEBUG] R2000 Header: after codePage bits=%d (byte=%d)\n", input.position(), input.position()/8);

        // 8. RL (4 바이트): Number of sections
        // LibreDWG header.spec: PRE(R_2004a) { FIELD_RL (sections, 0); }
        int sectionCount = input.readRawLong();

        // 9. 섹션 Locator 배열 읽기
        // LibreDWG decode.c line 327-329: RC + RL + RL = 9 bytes per locator
        // Format:
        // - record_number: RC (1 byte)
        // - seeker: RL (4 bytes) - byte offset in file
        // - size: RL (4 bytes) - byte length

        Map<String, Long> offsets = new HashMap<>();
        Map<String, Long> sizes = new HashMap<>();

        java.util.List<io.dwg.format.r13.R13SectionLocator> locators = new java.util.ArrayList<>();

        String[] sectionNames = {
            io.dwg.format.common.SectionType.HEADER.sectionName(),
            io.dwg.format.common.SectionType.CLASSES.sectionName(),
            io.dwg.format.common.SectionType.HANDLES.sectionName(),
            io.dwg.format.common.SectionType.OBJECTS.sectionName()
        };

        System.out.printf("[DEBUG] R2000: sectionCount=%d, position before locators: byte=%d\n",
            sectionCount, input.position()/8);
        for (int i = 0; i < sectionCount; i++) {
            long beforePos = input.position() / 8;
            int number = input.readRawChar();          // RC (1 byte) - was RL (BUG FIX)
            long address = input.readRawLong() & 0xFFFFFFFFL;  // RL (4 bytes)
            long size = input.readRawLong() & 0xFFFFFFFFL;     // RL (4 bytes)

            System.out.printf("[DEBUG] R2000 locator[%d] @byte%d: number=%d, address=0x%X, size=%d\n",
                i, beforePos, number, address, size);

            io.dwg.format.r13.R13SectionLocator locator = new io.dwg.format.r13.R13SectionLocator(number, address, size);
            locators.add(locator);

            // Only map standard sections (0-3) to their names
            // Skip auxiliary sections for now (records 4+)
            if (number >= 0 && number < sectionNames.length) {
                offsets.put(sectionNames[number], address);
                sizes.put(sectionNames[number], size);
                System.out.printf("[DEBUG] R2000: mapped to '%s'\n", sectionNames[number]);
            }
        }

        // 10. RS (2 바이트): CRC (skip for now, not validated)
        input.readRawShort();

        // R2000 Objects handling:
        // In R2000, Objects are NOT a separate section with fixed start/end.
        // Each object is located at an offset pointed to by Handles section entries.
        // We remove the bogus "AcDb:AcDbObjects" entry (locator[3] with size=0)
        // and let ObjectsSectionParser use handle offsets to locate objects in the file.
        offsets.remove(io.dwg.format.common.SectionType.OBJECTS.sectionName());
        sizes.remove(io.dwg.format.common.SectionType.OBJECTS.sectionName());

        fields.setSectionOffsets(offsets);
        fields.setSectionSizes(sizes);
        fields.setSectionLocators(locators);

        return fields;
    }

    @Override
    public Map<String, SectionInputStream> readSections(BitInput input, FileHeaderFields header) throws Exception {
        Map<String, SectionInputStream> sections = new HashMap<>();
        Map<String, Long> offsets = header.sectionOffsets();
        Map<String, Long> sizes = header.sectionSizes();

        for (String sectionName : offsets.keySet()) {
            long offset = offsets.get(sectionName);
            long size = sizes.get(sectionName);

            // Skip invalid sections
            if (size <= 0 || offset < 0) {
                continue;
            }

            if (offset >= 0 && size > 0) {
                input.seek(offset * 8); // Convert byte offset to bit offset

                // Read section data (fixed size)
                byte[] sectionData = new byte[(int) size];
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

        // Write section locators (use R13 format which is also valid for R2000)
        if (header.sectionLocators() != null) {
            for (Object locObj : header.sectionLocators()) {
                if (locObj instanceof io.dwg.format.r13.R13SectionLocator loc) {
                    loc.write(output);
                }
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
