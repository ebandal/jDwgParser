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
        R13FileHeaderReader.Header h = R13FileHeaderReader.read(input, id -> id.startsWith("AC101"), "R13");

        FileHeaderFields fields = new FileHeaderFields(DwgVersion.R13);
        fields.setPreviewOffset(h.imageSeeker);
        fields.setMaintenanceVersion(h.maintenanceVersion);
        fields.setCodePage(h.codePage);

        Map<String, Long> offsets = new HashMap<>();
        Map<String, Long> sizes = new HashMap<>();
        for (R13SectionLocator locator : h.locators) {
            offsets.put(locator.toSectionName(), locator.seeker());
            sizes.put(locator.toSectionName(), locator.size());
        }
        fields.setSectionOffsets(offsets);
        fields.setSectionSizes(sizes);
        return fields;
    }

    @Override
    public Map<String, SectionInputStream> readSections(BitInput input, FileHeaderFields header) throws Exception {
        Map<String, SectionInputStream> sections = new HashMap<>();

        if (header.sectionOffsets() == null || header.sectionOffsets().isEmpty()) {
            return sections;
        }

        if (header.sectionSizes() == null) {
            return sections;
        }

        for (String sectionName : header.sectionOffsets().keySet()) {
            try {
                long offset = header.sectionOffsets().get(sectionName);
                long size = header.sectionSizes().get(sectionName);

                if (offset == 0 || size == 0) {
                    continue;
                }

                input.seek(offset * 8);

                // Read full section bytes — Header/Classes parsers read their own sentinels
                byte[] sectionData = new byte[(int) size];
                for (int i = 0; i < size; i++) sectionData[i] = (byte) input.readRawChar();

                sections.put(sectionName, new SectionInputStream(sectionData, sectionName));

            } catch (Exception e) {
                // Skip sections that fail to read
            }
        }

        return sections;
    }

    @Override
    public void writeHeader(BitOutput output, FileHeaderFields header) throws Exception {
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
    }

    @Override
    public void writeSections(BitOutput output, Map<String, byte[]> sections, FileHeaderFields header) throws Exception {
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

            for (int i = 0; i < 16; i++) {
                output.writeRawChar(0);
            }

            for (byte b : data) {
                output.writeRawChar(b & 0xFF);
            }

            for (int i = 0; i < 16; i++) {
                output.writeRawChar(0);
            }

            output.writeRawShort((short) 0);
        }
    }
}
