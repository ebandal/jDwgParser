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
import io.dwg.entities.DwgObject;

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

        // [DEBUG] sections map 내용 출력
        System.out.printf("[DEBUG] ==== Sections map contents (%d entries) ====\n", sections.size());
        for (Map.Entry<String, SectionInputStream> e : sections.entrySet()) {
            System.out.printf("[DEBUG]   '%s' -> %d bytes\n", e.getKey(), e.getValue().rawBytes().length);
        }
        System.out.printf("[DEBUG] ==== Header section offsets ====\n");
        for (Map.Entry<String, Long> e : headerFields.sectionOffsets().entrySet()) {
            Long size = headerFields.sectionSizes().get(e.getKey());
            System.out.printf("[DEBUG]   '%s' -> offset=0x%X, size=%d\n",
                e.getKey(), e.getValue(), size != null ? size : -1);
        }

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
            try {
                List<DwgClassDefinition> classes =
                    new ClassesSectionParser().parse(classStream, version);
                doc.setCustomClasses(classes);
                classes.forEach(classRegistry::register);
                System.out.printf("[DEBUG] Classes parsed: %d classes\n", classes.size());
            } catch (Exception e) {
                System.out.printf("[WARN] Failed to parse Classes section: %s\n", e.getMessage());
                e.printStackTrace();
            }
        }
        doc.setClassRegistry(classRegistry);

        // ⑦ Handles 섹션 파싱
        HandleRegistry handleRegistry = new HandleRegistry();
        SectionInputStream handlesStream = sections.get("AcDb:Handles");
        if (handlesStream != null) {
            try {
                System.out.printf("[DEBUG] Parsing Handles section: %d bytes\n", handlesStream.rawBytes().length);
                handleRegistry = new HandlesSectionParser().parse(handlesStream, version);
                System.out.printf("[DEBUG] Handles parsed: %d entries\n", handleRegistry.allHandles().size());
            } catch (Exception e) {
                System.out.printf("[WARN] Failed to parse Handles section: %s\n", e.getMessage());
                e.printStackTrace();
            }
        }
        doc.setHandleRegistry(handleRegistry);

        // ⑧ Objects 섹션 파싱
        // Try both naming conventions (R2007+ uses AcDb:AcDbObjects, R2004 uses AcDb:Objects)
        SectionInputStream objectsStream = sections.get("AcDb:AcDbObjects");
        if (objectsStream == null) {
            objectsStream = sections.get("AcDb:Objects");
        }
        // R2000: Objects are not a separate section - they are located via Handles offsets
        // in the entire file. Use the whole file as the pseudo-section.
        if (objectsStream == null && version == DwgVersion.R2000 && !handleRegistry.allHandles().isEmpty()) {
            System.out.printf("[DEBUG] DwgReader: R2000 - using whole file as Objects stream (size=%d)\n", data.length);
            objectsStream = new SectionInputStream(data, "AcDb:AcDbObjects");
        }
        System.out.printf("[DEBUG] DwgReader: objectsStream found: %b\n", objectsStream != null);
        if (objectsStream != null) {
            System.out.printf("[DEBUG] DwgReader: objectsStream size: %d bytes\n", objectsStream.rawBytes().length);
            ObjectsSectionParser objParser = new ObjectsSectionParser();
            objParser.setHandleRegistry(handleRegistry);
            objParser.setClassRegistry(classRegistry);
            System.out.printf("[DEBUG] DwgReader: handleRegistry empty: %b\n", handleRegistry.allHandles().isEmpty());
            Map<Long, DwgObject> objectMap = objParser.parse(objectsStream, version);
            System.out.printf("[DEBUG] DwgReader: parsed %d objects\n", objectMap.size());
            doc.setObjectMap(objectMap);
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
