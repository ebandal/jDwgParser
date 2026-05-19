package io.dwg.api;

import io.dwg.core.io.BitOutput;
import io.dwg.core.io.ByteBufferBitOutput;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.DwgFileStructureHandler;
import io.dwg.format.common.DwgFileStructureHandlerFactory;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.format.common.SectionType;
import io.dwg.format.r13.R13SectionLocator;
import io.dwg.sections.classes.ClassesSectionWriter;
import io.dwg.sections.handles.HandlesSectionWriter;
import io.dwg.sections.header.HeaderSectionWriter;
import io.dwg.sections.objects.ObjectsSectionWriter;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DWG 파일 쓰기 진입점.
 */
public class DwgWriter {
    private final DwgVersion targetVersion;

    private DwgWriter(DwgVersion version) {
        this.targetVersion = version;
    }

    public static DwgWriter forVersion(DwgVersion version) {
        return new DwgWriter(version);
    }

    /** DwgDocument를 파일로 저장 */
    public void write(DwgDocument document, Path filePath) throws Exception {
        byte[] bytes = toBytes(document);
        Files.write(filePath, bytes);
    }

    /** DwgDocument를 스트림으로 저장 */
    public void write(DwgDocument document, OutputStream stream) throws Exception {
        stream.write(toBytes(document));
    }

    /** DwgDocument를 바이트 배열로 직렬화 */
    public byte[] toBytes(DwgDocument document) throws Exception {
        DwgVersion version = targetVersion != null ? targetVersion : document.version();
        DwgFileStructureHandler handler = DwgFileStructureHandlerFactory.forVersion(version);

        FileHeaderFields headerFields = new FileHeaderFields(version);

        Map<String, byte[]> sections = new HashMap<>();

        if (document.header() != null) {
            HeaderSectionWriter headerWriter = new HeaderSectionWriter();
            sections.put(headerWriter.sectionName(),
                headerWriter.write(document.header(), version).toByteArray());
        }

        if (document.customClasses() != null && !document.customClasses().isEmpty()) {
            ClassesSectionWriter classesWriter = new ClassesSectionWriter();
            sections.put(classesWriter.sectionName(),
                classesWriter.write(document.customClasses(), version).toByteArray());
        }

        if (document.handleRegistry() != null) {
            HandlesSectionWriter handlesWriter = new HandlesSectionWriter();
            sections.put(handlesWriter.sectionName(),
                handlesWriter.write(document.handleRegistry(), version).toByteArray());
        }

        ObjectsSectionWriter objectsWriter = new ObjectsSectionWriter();
        sections.put(objectsWriter.sectionName(),
            objectsWriter.write(document.objectMap(), version).toByteArray());

        if (version == DwgVersion.R13 || version == DwgVersion.R14 || version == DwgVersion.R2000) {
            String[] order = {
                SectionType.HEADER.sectionName(),
                SectionType.CLASSES.sectionName(),
                SectionType.HANDLES.sectionName(),
                SectionType.OBJECTS.sectionName()
            };
            int sectionCount = 0;
            for (String name : order) {
                if (sections.containsKey(name)) sectionCount++;
            }
            int headerSize = 24 + sectionCount * 12;
            long currentOffset = headerSize;

            List<R13SectionLocator> locators = new ArrayList<>();
            int recordNum = 0;
            for (String name : order) {
                byte[] data = sections.getOrDefault(name, new byte[0]);
                long totalSize = data.length + 34; // 16B+data+16B+2B
                locators.add(new R13SectionLocator(recordNum++, currentOffset, totalSize));
                currentOffset += totalSize;
            }
            headerFields.setSectionLocators(locators);
        } else if (version.isR2004OrLater()) {
            long totalSectionSize = 0;
            for (byte[] sectionData : sections.values()) {
                totalSectionSize += sectionData.length;
            }

            if (version.isR2007OrLater()) {
                long baseOffset = 0x480;
                long sectionMapPageId = 1;
                long estimatedSectionMapSize = Math.max(512, totalSectionSize / 10);
                long pageMapOffset = baseOffset + totalSectionSize + estimatedSectionMapSize;

                headerFields.setPageMapOffset(pageMapOffset);
                headerFields.setSectionMapId(sectionMapPageId);
            } else {
                long sectionMapOffset = 0x100 + totalSectionSize;
                headerFields.setSectionMapOffset(sectionMapOffset);
            }
        }

        BitOutput output = new ByteBufferBitOutput();
        handler.writeHeader(output, headerFields);
        handler.writeSections(output, sections, headerFields);

        return output.toByteArray();
    }
}
