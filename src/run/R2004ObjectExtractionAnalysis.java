package run;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.core.version.DwgVersionDetector;
import io.dwg.format.common.DwgFileStructureHandler;
import io.dwg.format.common.DwgFileStructureHandlerFactory;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.sections.classes.ClassesSectionParser;
import io.dwg.sections.classes.DwgClassRegistry;
import io.dwg.sections.handles.HandleRegistry;
import io.dwg.sections.handles.HandlesSectionParser;
import io.dwg.sections.objects.ObjectsSectionParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * R2004 객체 추출 분석 및 진단 도구
 */
public class R2004ObjectExtractionAnalysis {
    public static void main(String[] args) throws Exception {
        String[] testFiles = {
                "samples/example_2004.dwg",
                "samples/Arc_2004.dwg"
        };

        for (String filePath : testFiles) {
            analyzeFile(filePath);
        }
    }

    private static void analyzeFile(String filePath) throws Exception {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("분석: " + filePath);
        System.out.println("=".repeat(100));

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            System.out.println("❌ 파일 없음: " + path.toAbsolutePath());
            return;
        }

        byte[] data = Files.readAllBytes(path);
        System.out.printf("✓ 파일 로드: %,d 바이트\n", data.length);

        // Step 1: Version detection
        DwgVersion version = DwgVersionDetector.detect(data);
        System.out.println("✓ 버전: " + version);

        // Step 2: Get handler and read header
        DwgFileStructureHandler handler = DwgFileStructureHandlerFactory.forVersion(version);
        System.out.println("✓ 핸들러: " + handler.getClass().getSimpleName());

        BitInput input = new ByteBufferBitInput(data);
        FileHeaderFields headerFields = handler.readHeader(input);
        System.out.println("✓ 헤더 파싱 성공");

        // Step 3: Read sections
        System.out.println("\n[섹션 추출]");
        input = new ByteBufferBitInput(data);
        Map<String, SectionInputStream> sections = handler.readSections(input, headerFields);

        System.out.printf("✓ 추출된 섹션: %d개\n", sections.size());
        sections.forEach((name, stream) -> {
            System.out.printf("  - %s: %,d 바이트\n", name, stream.size());
        });

        // Step 4: Analyze Handles section
        System.out.println("\n[Handles 섹션 분석]");
        SectionInputStream handlesStream = sections.get("AcDb:Handles");
        if (handlesStream == null) {
            System.out.println("❌ Handles 섹션 NOT FOUND!");
            System.out.println("   가능한 섹션: " + String.join(", ", sections.keySet()));
        } else {
            System.out.printf("✓ Handles 섹션 발견: %,d 바이트\n", handlesStream.size());

            // Parse handles
            HandleRegistry handleRegistry = new HandleRegistry();
            try {
                handleRegistry = new HandlesSectionParser().parse(handlesStream, version);
                System.out.printf("✓ Handles 파싱 완료\n");
                System.out.printf("  - Handle 개수: %d\n", handleRegistry.allHandles().size());

                // Show first few handles
                int count = 0;
                for (long handle : handleRegistry.allHandles()) {
                    if (count++ >= 5) break;
                    var offset = handleRegistry.offsetFor(handle);
                    if (offset.isPresent()) {
                        System.out.printf("    - Handle 0x%04X -> 오프셋 0x%04X\n", handle, offset.get());
                    }
                }
                if (handleRegistry.allHandles().size() > 5) {
                    System.out.printf("    ... 그 외 %d개 더\n", handleRegistry.allHandles().size() - 5);
                }
            } catch (Exception e) {
                System.out.printf("❌ Handles 파싱 실패: %s\n", e.getMessage());
                handleRegistry = new HandleRegistry();
            }

            // Step 5: Analyze Objects section
            System.out.println("\n[Objects 섹션 분석]");
            SectionInputStream objectsStream = sections.get("AcDb:AcDbObjects");
            if (objectsStream == null) {
                System.out.println("❌ Objects 섹션 NOT FOUND!");
            } else {
                System.out.printf("✓ Objects 섹션 발견: %,d 바이트\n", objectsStream.size());

                // Try to parse objects
                System.out.println("\n[객체 파싱]");
                try {
                    DwgClassRegistry classRegistry = new DwgClassRegistry();
                    SectionInputStream classStream = sections.get("AcDb:Classes");
                    if (classStream != null) {
                        ClassesSectionParser classParser = new ClassesSectionParser();
                        var classes = classParser.parse(classStream, version);
                        classes.forEach(classRegistry::register);
                        System.out.printf("✓ Classes 파싱: %d개 클래스\n", classes.size());
                    } else {
                        System.out.println("⚠ Classes 섹션 없음");
                    }

                    ObjectsSectionParser objParser = new ObjectsSectionParser();
                    objParser.setHandleRegistry(handleRegistry);
                    objParser.setClassRegistry(classRegistry);
                    Map<Long, ?> objectMap = objParser.parse(objectsStream, version);

                    System.out.printf("✓ 파싱된 객체: %d개\n", objectMap.size());
                    if (objectMap.isEmpty()) {
                        System.out.println("\n❌ 문제: Objects 섹션이 파싱되지 않았습니다!");
                        diagnoseEmptyObjects(objectsStream, version, handleRegistry);
                    } else {
                        // Show first few objects
                        int count = 0;
                        for (var entry : objectMap.entrySet()) {
                            if (count++ >= 5) break;
                            System.out.printf("  - Handle 0x%04X: %s\n",
                                entry.getKey(), entry.getValue().getClass().getSimpleName());
                        }
                        if (objectMap.size() > 5) {
                            System.out.printf("  ... 그 외 %d개 더\n", objectMap.size() - 5);
                        }
                    }
                } catch (Exception e) {
                    System.out.printf("❌ 객체 파싱 실패: %s\n", e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        // Step 6: Try DwgReader API
        System.out.println("\n[DwgReader API 테스트]");
        try {
            DwgDocument doc = DwgReader.defaultReader().open(path);
            System.out.printf("✓ DwgReader.open() 성공\n");
            System.out.printf("  - 버전: %s\n", doc.version());
            System.out.printf("  - 객체: %d개\n", doc.objectMap().size());

            if (doc.objectMap().isEmpty()) {
                System.out.println("  ❌ 객체맵이 비어있음");
            }
        } catch (Exception e) {
            System.out.printf("❌ DwgReader 실패: %s\n", e.getMessage());
        }
    }

    private static void diagnoseEmptyObjects(SectionInputStream objectsStream,
            DwgVersion version, HandleRegistry handleRegistry) {
        System.out.println("\n[진단: 빈 Objects]");

        if (handleRegistry.allHandles().isEmpty()) {
            System.out.println("✗ HandleRegistry가 비어있음 → ObjectsSectionParser 반환 조건");
            System.out.println("  → HandlesSectionParser가 handles를 파싱하지 못했을 가능성");
        } else {
            System.out.printf("✗ HandleRegistry에는 %d개 handle이 있지만 객체는 못 찾음\n",
                handleRegistry.allHandles().size());
            System.out.println("  → 가능한 원인:");
            System.out.println("    1. Objects 섹션의 바이트 레이아웃이 예상과 다름");
            System.out.println("    2. Handle-Offset 매핑이 잘못됨");
            System.out.println("    3. 객체 타입 코드를 인식하지 못함");
        }

        System.out.printf("\n  Objects 섹션 크기: %d 바이트\n", objectsStream.size());
        System.out.printf("  HandleRegistry: %d handles\n", handleRegistry.allHandles().size());
        System.out.printf("  버전: %s\n", version);
    }
}
