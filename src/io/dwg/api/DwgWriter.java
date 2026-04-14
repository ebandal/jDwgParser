package io.dwg.api;

import io.dwg.core.io.BitOutput;
import io.dwg.core.io.ByteBufferBitOutput;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.DwgFileStructureHandler;
import io.dwg.format.common.DwgFileStructureHandlerFactory;
import io.dwg.format.common.FileHeaderFields;
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

        // Objects 섹션 작성
        ObjectsSectionWriter objectsWriter = new ObjectsSectionWriter();
        sections.put(objectsWriter.sectionName(),
            objectsWriter.write(document.objectMap(), version).toByteArray());

        BitOutput output = new ByteBufferBitOutput();
        handler.writeHeader(output, headerFields);
        handler.writeSections(output, sections, headerFields);

        return output.toByteArray();
    }
}
