package io.dwg.api;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.core.version.DwgVersionDetector;
import io.dwg.format.common.DwgFileStructureHandler;
import io.dwg.format.common.DwgFileStructureHandlerFactory;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.sections.classes.ClassesSectionParser;
import io.dwg.sections.classes.DwgClassDefinition;
import io.dwg.sections.classes.DwgClassRegistry;
import io.dwg.sections.handles.HandleRegistry;
import io.dwg.sections.handles.HandlesSectionParser;
import io.dwg.sections.header.HeaderSectionParser;
import io.dwg.sections.header.HeaderVariables;
import io.dwg.sections.objects.ObjectsSectionParser;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * DWG 파일 읽기 진입점. Fluent Builder 패턴.
 */
public class DwgReader {
    private DwgReader() {}

    public static DwgReader defaultReader() {
        return new DwgReader();
    }

    /** 파일 경로에서 DWG 파일 읽기 */
    public DwgDocument open(Path filePath) throws Exception {
        byte[] bytes = Files.readAllBytes(filePath);
        return open(bytes);
    }

    /** 스트림에서 DWG 파일 읽기 (전체 메모리 로드) */
    public DwgDocument open(InputStream stream) throws Exception {
        byte[] bytes = stream.readAllBytes();
        return open(bytes);
    }

    /** 바이트 배열에서 DWG 파일 읽기 */
    public DwgDocument open(byte[] data) throws Exception {
        // ① 버전 감지
        DwgVersion version = DwgVersionDetector.detect(data);
        DwgDocument doc = new DwgDocument(version);

        // ② 포맷 핸들러 선택
        DwgFileStructureHandler handler = DwgFileStructureHandlerFactory.forVersion(version);
        BitInput input = new ByteBufferBitInput(data);

        // ③ 헤더 파싱
        FileHeaderFields headerFields = handler.readHeader(input);

        // ④ 섹션 추출
        input = new ByteBufferBitInput(data); // 처음부터 다시
        Map<String, SectionInputStream> sections = handler.readSections(input, headerFields);

        // ⑤ Header 섹션 파싱
        SectionInputStream headerStream = sections.get("AcDb:Header");
        if (headerStream != null) {
            HeaderVariables vars = new HeaderSectionParser().parse(headerStream, version);
            doc.setHeaderVariables(vars);
        }

        // ⑥ Classes 섹션 파싱
        DwgClassRegistry classRegistry = new DwgClassRegistry();
        SectionInputStream classStream = sections.get("AcDb:Classes");
        if (classStream != null) {
            List<DwgClassDefinition> classes =
                new ClassesSectionParser().parse(classStream, version);
            doc.setCustomClasses(classes);
            classes.forEach(classRegistry::register);
        }
        doc.setClassRegistry(classRegistry);

        // ⑦ Handles 섹션 파싱
        HandleRegistry handleRegistry = new HandleRegistry();
        SectionInputStream handlesStream = sections.get("AcDb:Handles");
        if (handlesStream != null) {
            handleRegistry = new HandlesSectionParser().parse(handlesStream, version);
        }
        doc.setHandleRegistry(handleRegistry);

        // ⑧ Objects 섹션 파싱
        SectionInputStream objectsStream = sections.get("AcDb:AcDbObjects");
        if (objectsStream != null) {
            ObjectsSectionParser objParser = new ObjectsSectionParser();
            objParser.setHandleRegistry(handleRegistry);
            objParser.setClassRegistry(classRegistry);
            doc.setObjectMap(objParser.parse(objectsStream, version));
        }

        return doc;
    }

    /** 버전만 빠르게 감지 */
    public DwgVersion detectVersion(Path filePath) throws Exception {
        byte[] header = new byte[6];
        try (var stream = Files.newInputStream(filePath)) {
            stream.read(header);
        }
        return DwgVersionDetector.detect(header);
    }
}
