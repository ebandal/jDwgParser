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

        // 섹션 직렬화
        Map<String, byte[]> sections = new HashMap<>();

        // Header 섹션 작성
        if (document.header() != null) {
            HeaderSectionWriter headerWriter = new HeaderSectionWriter();
            sections.put(headerWriter.sectionName(),
                headerWriter.write(document.header(), version).toByteArray());
        }

        // Classes 섹션 작성
        if (document.customClasses() != null && !document.customClasses().isEmpty()) {
            ClassesSectionWriter classesWriter = new ClassesSectionWriter();
            sections.put(classesWriter.sectionName(),
                classesWriter.write(document.customClasses(), version).toByteArray());
        }

        // Handles 섹션 작성
        if (document.handleRegistry() != null) {
            HandlesSectionWriter handlesWriter = new HandlesSectionWriter();
            sections.put(handlesWriter.sectionName(),
                handlesWriter.write(document.handleRegistry(), version).toByteArray());
        }

        // Objects 섹션 작성
        ObjectsSectionWriter objectsWriter = new ObjectsSectionWriter();
        sections.put(objectsWriter.sectionName(),
            objectsWriter.write(document.objectMap(), version).toByteArray());

        System.out.println("[DEBUG] DwgWriter: version=" + version + ", sections=" + sections.size());
        for (String sectionName : sections.keySet()) {
            byte[] data = sections.get(sectionName);
            System.out.println("  [DEBUG] Section '" + sectionName + "': " + data.length + " bytes");
        }

        // R13/R14/R2000: 섹션 로케이터 사전 계산
        long sectionMapOffset = 0;
        if (version == DwgVersion.R13 || version == DwgVersion.R14 || version == DwgVersion.R2000) {
            String[] order = {
                SectionType.HEADER.sectionName(),
                SectionType.CLASSES.sectionName(),
                SectionType.HANDLES.sectionName(),
                SectionType.OBJECTS.sectionName()
            };
            // Header size = 6+6+1+4+1+1+2+1 + sectionCount×12 + 2
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
            System.out.println("[DEBUG] R13/R14/R2000: Locators for " + sectionCount + " sections calculated");
        }
        // R2004+: 섹션맵 오프셋 계산
        else if (version.isR2004OrLater()) {
            long totalSectionSize = 0;
            for (byte[] sectionData : sections.values()) {
                totalSectionSize += sectionData.length;
            }

            if (version.isR2007OrLater()) {
                // R2007+ uses page-based addressing with Page Map
                // Data layout: [0x480 header] [section pages] [section map page] [page map]
                long baseOffset = 0x480;  // Header size in R2007

                // Section Map will be page 1
                long sectionMapPageId = 1;

                // Page Map will start after section data and section map page
                // Estimate section map page size (will be LZ77 compressed)
                long estimatedSectionMapSize = Math.max(512, totalSectionSize / 10);  // rough estimate
                long pageMapOffset = baseOffset + totalSectionSize + estimatedSectionMapSize;

                headerFields.setPageMapOffset(pageMapOffset);
                headerFields.setSectionMapId(sectionMapPageId);
                System.out.println("[DEBUG] R2007: pageMapOffset=0x" + Long.toHexString(pageMapOffset) +
                    ", sectionMapId=" + sectionMapPageId);
            } else {
                // R2004: direct offset to section map
                sectionMapOffset = 0x100 + totalSectionSize;
                headerFields.setSectionMapOffset(sectionMapOffset);
                System.out.println("[DEBUG] R2004: Calculated section map offset: 0x" + Long.toHexString(sectionMapOffset));
            }
        }

        BitOutput output = new ByteBufferBitOutput();
        handler.writeHeader(output, headerFields);
        System.out.println("[DEBUG] After writeHeader: output position=" + output.position());
        handler.writeSections(output, sections, headerFields);
        System.out.println("[DEBUG] After writeSections: output position=" + output.position());

        byte[] result = output.toByteArray();
        System.out.println("[DEBUG] Final output: " + result.length + " bytes");
        return result;
    }
}
