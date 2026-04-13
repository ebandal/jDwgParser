package io.dwg.test;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.core.version.DwgVersionDetector;
import io.dwg.entities.DwgEntity;
import io.dwg.entities.concrete.DwgLayer;
import io.dwg.format.common.DwgFileStructureHandler;
import io.dwg.format.common.DwgFileStructureHandlerFactory;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.sections.classes.ClassesSectionParser;
import io.dwg.sections.classes.DwgClassDefinition;
import io.dwg.sections.handles.HandleRegistry;
import io.dwg.sections.handles.HandlesSectionParser;
import io.dwg.sections.header.HeaderSectionParser;
import io.dwg.sections.header.HeaderVariables;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * DWG 파일 파싱 단계별 테스트.
 * 각 단계에서 실패 지점을 명확히 파악하기 위한 구조화된 테스트.
 */
public class DwgParsingStageTest {

    public static void main(String[] args) {
        // 샘플 DWG 파일 경로 (사용자가 제공해야 함)
        String dwgFilePath = args.length > 0 ? args[0] : "./sample.dwg";

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  DWG 파일 파싱 단계별 테스트");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("대상 파일: " + dwgFilePath);
        System.out.println();

        try {
            byte[] fileBytes = readFileBytes(dwgFilePath);
            System.out.printf("[✓] STAGE 1: 파일 읽기 성공 (%d bytes)\n\n", fileBytes.length);

            // ================================================================
            // STAGE 2: 버전 감지
            // ================================================================
            testStage2VersionDetection(fileBytes);

            // ================================================================
            // STAGE 3: 헤더 파싱
            // ================================================================
            testStage3HeaderParsing(fileBytes);

            // ================================================================
            // STAGE 4: 섹션 추출
            // ================================================================
            testStage4SectionExtraction(fileBytes);

            // ================================================================
            // STAGE 5: 각 섹션 파싱
            // ================================================================
            testStage5SectionParsing(fileBytes);

            // ================================================================
            // STAGE 6: 전체 문서 파싱 (고수준 API)
            // ================================================================
            testStage6FullDocumentParsing(dwgFilePath);

            // ================================================================
            // 최종 결과
            // ================================================================
            System.out.println();
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println("  [✓] 모든 단계 파싱 완료");
            System.out.println("═══════════════════════════════════════════════════════════════");

        } catch (Exception e) {
            System.out.println();
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println("  [✗] 파싱 실패");
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println("오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================================================================
    // STAGE 2: 버전 감지
    // ================================================================
    private static void testStage2VersionDetection(byte[] fileBytes) throws Exception {
        System.out.println("─────────────────────────────────────────────────────────────");
        System.out.println("STAGE 2: 버전 감지");
        System.out.println("─────────────────────────────────────────────────────────────");

        try {
            DwgVersion version = DwgVersionDetector.detect(fileBytes);
            System.out.printf("  ✓ 감지된 버전: %s (%s)\n", version.name(), version.versionString());
            System.out.println();
        } catch (Exception e) {
            System.out.printf("  ✗ 버전 감지 실패: %s\n", e.getMessage());
            throw e;
        }
    }

    // ================================================================
    // STAGE 3: 헤더 파싱
    // ================================================================
    private static void testStage3HeaderParsing(byte[] fileBytes) throws Exception {
        System.out.println("─────────────────────────────────────────────────────────────");
        System.out.println("STAGE 3: 헤더 파싱");
        System.out.println("─────────────────────────────────────────────────────────────");

        DwgVersion version = DwgVersionDetector.detect(fileBytes);
        DwgFileStructureHandler handler = DwgFileStructureHandlerFactory.forVersion(version);
        BitInput input = new ByteBufferBitInput(fileBytes);

        try {
            FileHeaderFields headerFields = handler.readHeader(input);
            System.out.printf("  ✓ 헤더 파싱 성공\n");
            System.out.printf("    - 버전: %s\n", headerFields.version());
            System.out.printf("    - 유지보수 버전: %d\n", headerFields.maintenanceVersion());
            System.out.printf("    - 코드페이지: %d\n", headerFields.codePage());
            System.out.printf("    - 보안 플래그: 0x%08X %s\n",
                headerFields.securityFlags(),
                headerFields.isEncrypted() ? "(암호화됨)" : "(암호화 안됨)");
            System.out.println();
        } catch (Exception e) {
            System.out.printf("  ✗ 헤더 파싱 실패: %s\n", e.getMessage());
            throw e;
        }
    }

    // ================================================================
    // STAGE 4: 섹션 추출
    // ================================================================
    private static void testStage4SectionExtraction(byte[] fileBytes) throws Exception {
        System.out.println("─────────────────────────────────────────────────────────────");
        System.out.println("STAGE 4: 섹션 추출");
        System.out.println("─────────────────────────────────────────────────────────────");

        DwgVersion version = DwgVersionDetector.detect(fileBytes);
        DwgFileStructureHandler handler = DwgFileStructureHandlerFactory.forVersion(version);
        BitInput input = new ByteBufferBitInput(fileBytes);

        FileHeaderFields headerFields = handler.readHeader(input);
        input = new ByteBufferBitInput(fileBytes); // 처음부터 다시

        try {
            Map<String, SectionInputStream> sections = handler.readSections(input, headerFields);
            System.out.printf("  ✓ 섹션 추출 성공 (%d개)\n", sections.size());
            for (String sectionName : sections.keySet()) {
                SectionInputStream si = sections.get(sectionName);
                System.out.printf("    - %s (%d bytes)\n", sectionName, si.size());
            }
            System.out.println();
        } catch (Exception e) {
            System.out.printf("  ✗ 섹션 추출 실패: %s\n", e.getMessage());
            throw e;
        }
    }

    // ================================================================
    // STAGE 5: 각 섹션 파싱
    // ================================================================
    private static void testStage5SectionParsing(byte[] fileBytes) throws Exception {
        System.out.println("─────────────────────────────────────────────────────────────");
        System.out.println("STAGE 5: 개별 섹션 파싱");
        System.out.println("─────────────────────────────────────────────────────────────");

        DwgVersion version = DwgVersionDetector.detect(fileBytes);
        DwgFileStructureHandler handler = DwgFileStructureHandlerFactory.forVersion(version);
        BitInput input = new ByteBufferBitInput(fileBytes);

        FileHeaderFields headerFields = handler.readHeader(input);
        input = new ByteBufferBitInput(fileBytes);
        Map<String, SectionInputStream> sections = handler.readSections(input, headerFields);

        // 5-1: Header 섹션
        testSection5_1Header(sections, version);

        // 5-2: Classes 섹션
        testSection5_2Classes(sections, version);

        // 5-3: Handles 섹션
        testSection5_3Handles(sections, version);

        System.out.println();
    }

    private static void testSection5_1Header(Map<String, SectionInputStream> sections,
            DwgVersion version) {
        System.out.println("  [5-1] Header 섹션");
        try {
            SectionInputStream stream = sections.get("AcDb:Header");
            if (stream == null) {
                System.out.println("    ⚠ Header 섹션 없음");
                return;
            }
            HeaderVariables vars = new HeaderSectionParser().parse(stream, version);
            System.out.println("    ✓ 파싱 성공");
            System.out.printf("      - DIMSCALE: %.2f\n", vars.dimscale());
            System.out.printf("      - LTSCALE: %.2f\n", vars.ltscale());
            System.out.printf("      - LUNITS: %d\n", vars.lunits());
        } catch (Exception e) {
            System.out.printf("    ✗ 파싱 실패: %s\n", e.getMessage());
        }
    }

    private static void testSection5_2Classes(Map<String, SectionInputStream> sections,
            DwgVersion version) {
        System.out.println("  [5-2] Classes 섹션");
        try {
            SectionInputStream stream = sections.get("AcDb:Classes");
            if (stream == null) {
                System.out.println("    ⚠ Classes 섹션 없음");
                return;
            }
            List<DwgClassDefinition> classes = new ClassesSectionParser().parse(stream, version);
            System.out.println("    ✓ 파싱 성공");
            System.out.printf("      - 클래스 수: %d\n", classes.size());
            if (!classes.isEmpty()) {
                DwgClassDefinition first = classes.get(0);
                System.out.printf("      - 첫 번째 클래스: %s (number=%d)\n",
                    first.dxfRecordName(), first.classNumber());
            }
        } catch (Exception e) {
            System.out.printf("    ✗ 파싱 실패: %s\n", e.getMessage());
        }
    }

    private static void testSection5_3Handles(Map<String, SectionInputStream> sections,
            DwgVersion version) {
        System.out.println("  [5-3] Handles 섹션");
        try {
            SectionInputStream stream = sections.get("AcDb:Handles");
            if (stream == null) {
                System.out.println("    ⚠ Handles 섹션 없음");
                return;
            }
            HandleRegistry registry = new HandlesSectionParser().parse(stream, version);
            System.out.println("    ✓ 파싱 성공");
            System.out.printf("      - 핸들 수: %d\n", registry.size());
        } catch (Exception e) {
            System.out.printf("    ✗ 파싱 실패: %s\n", e.getMessage());
        }
    }

    // ================================================================
    // STAGE 6: 전체 문서 파싱 (고수준 API)
    // ================================================================
    private static void testStage6FullDocumentParsing(String dwgFilePath) throws Exception {
        System.out.println("─────────────────────────────────────────────────────────────");
        System.out.println("STAGE 6: 전체 문서 파싱 (고수준 API)");
        System.out.println("─────────────────────────────────────────────────────────────");

        try {
            DwgDocument doc = DwgReader.defaultReader().open(Paths.get(dwgFilePath));

            System.out.println("  ✓ DwgDocument 생성 성공");
            System.out.printf("    - 버전: %s\n", doc.version());
            System.out.printf("    - 전체 객체 수: %d\n", doc.objectMap().size());

            List<DwgLayer> layers = doc.layers();
            System.out.printf("    - 레이어 수: %d\n", layers.size());
            if (!layers.isEmpty()) {
                System.out.println("      레이어 목록:");
                for (DwgLayer layer : layers.stream().limit(5).toList()) {
                    System.out.printf("        • %s (handle=0x%X)\n", layer.name(), layer.handle());
                }
                if (layers.size() > 5) {
                    System.out.printf("        ... 외 %d개\n", layers.size() - 5);
                }
            }

            List<DwgEntity> entities = doc.entities();
            System.out.printf("    - 엔티티 수: %d\n", entities.size());
            if (!entities.isEmpty()) {
                System.out.println("      엔티티 타입 분포:");
                Map<String, Integer> typeCounts = new java.util.HashMap<>();
                for (DwgEntity e : entities) {
                    String typeName = e.objectType().name();
                    typeCounts.put(typeName, typeCounts.getOrDefault(typeName, 0) + 1);
                }
                for (Map.Entry<String, Integer> entry : typeCounts.entrySet()
                    .stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(10).toList()) {
                    System.out.printf("        • %s: %d\n", entry.getKey(), entry.getValue());
                }
            }

            System.out.println();
        } catch (Exception e) {
            System.out.printf("  ✗ 파싱 실패: %s\n", e.getMessage());
            throw e;
        }
    }

    // ================================================================
    // 헬퍼 메서드
    // ================================================================
    private static byte[] readFileBytes(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!java.nio.file.Files.exists(path)) {
            throw new IOException("파일 없음: " + filePath);
        }
        return java.nio.file.Files.readAllBytes(path);
    }
}
