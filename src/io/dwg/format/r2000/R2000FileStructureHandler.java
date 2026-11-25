package io.dwg.format.r2000;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.BitOutput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.AbstractFileStructureHandler;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.format.common.SectionType;
import io.dwg.format.r13.R13FileHeaderReader;
import io.dwg.format.r13.R13SectionLocator;
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
        R13FileHeaderReader.Header h = R13FileHeaderReader.read(input, "AC1015"::equals, "R2000");

        FileHeaderFields fields = new FileHeaderFields(DwgVersion.R2000);
        fields.setPreviewOffset(h.imageSeeker);
        fields.setMaintenanceVersion(h.maintenanceVersion);
        fields.setCodePage(h.codePage);

        // Records 0-2 locate the header variables, the classes and the object map. The
        // objects themselves are found through the object map, so record 3 is not used here.
        String[] names = {
            SectionType.HEADER.sectionName(),
            SectionType.CLASSES.sectionName(),
            SectionType.HANDLES.sectionName()
        };
        Map<String, Long> offsets = new HashMap<>();
        Map<String, Long> sizes = new HashMap<>();
        for (R13SectionLocator locator : h.locators) {
            int number = locator.recordNumber();
            if (number < names.length) {
                offsets.put(names[number], locator.seeker());
                sizes.put(names[number], locator.size());
            }
        }
        fields.setSectionOffsets(offsets);
        fields.setSectionSizes(sizes);
        fields.setSectionLocators(h.locators);
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

            if (size <= 0 || offset < 0) {
                continue;
            }

            if (offset >= 0 && size > 0) {
                input.seek(offset * 8); // Convert byte offset to bit offset

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

        java.util.Map<String, Long> offsets = header.sectionOffsets();
        if (offsets != null && !offsets.isEmpty()) {
            for (String name : sectionNames) {
                if (offsets.containsKey(name)) {
                    sectionCount++;
                }
            }
        } else {
            sectionCount = sectionNames.length;
        }
        output.writeRawChar(sectionCount);

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
        String[] sectionOrder = {
            io.dwg.format.common.SectionType.HEADER.sectionName(),
            io.dwg.format.common.SectionType.CLASSES.sectionName(),
            io.dwg.format.common.SectionType.HANDLES.sectionName(),
            io.dwg.format.common.SectionType.OBJECTS.sectionName()
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
