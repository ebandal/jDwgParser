package io.dwg.api;

import io.dwg.core.io.BitOutput;
import io.dwg.core.io.ByteBufferBitOutput;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.DwgFileStructureHandler;
import io.dwg.format.common.DwgFileStructureHandlerFactory;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.sections.classes.ClassesSectionWriter;
import io.dwg.sections.handles.HandlesSectionWriter;
import io.dwg.sections.header.HeaderSectionWriter;
import io.dwg.sections.objects.ObjectsSectionWriter;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
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

        // R2004+: 섹션맵 오프셋 계산 (바이트 단위)
        long sectionMapOffset = 0;
        if (version.isR2004OrLater()) {
            long totalSectionSize = 0;
            for (byte[] sectionData : sections.values()) {
                totalSectionSize += sectionData.length;
            }
            // Section map offset = header size + all sections (byte offset)
            // Header is 0x100 bytes (6 version + 0x7A live data + 0x6C encrypted + 0x1A padding)
            sectionMapOffset = 0x100 + totalSectionSize;
            headerFields.setSectionMapOffset(sectionMapOffset);
            System.out.println("[DEBUG] Calculated section map offset: 0x" + Long.toHexString(sectionMapOffset));
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
