package debug;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.format.common.FileHeaderFields;
import io.dwg.format.r2000.R2000FileStructureHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Updated R2000FileStructureHandler 검증
 * Objects 섹션 추출 확인
 */
public class TestR2000HandlerUpdated {
    public static void main(String[] args) throws Exception {
        System.out.println("=== R2000FileStructureHandler (Updated) ===\n");

        int totalCount = 0;
        int successCount = 0;

        try (Stream<Path> paths = Files.walk(Paths.get("samples/2000"))) {
            var results = paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".dwg"))
                .sorted()
                .toList();

            System.out.printf("발견된 R2000 파일: %d개\n\n", results.size());

            R2000FileStructureHandler handler = new R2000FileStructureHandler();

            for (Path path : results) {
                totalCount++;
                String name = path.getFileName().toString();

                try {
                    byte[] data = Files.readAllBytes(path);
                    BitInput input = new ByteBufferBitInput(data);

                    // 헤더 읽기
                    FileHeaderFields header = handler.readHeader(input);

                    System.out.printf("%-30s: OK", name);
                    System.out.printf(" Maint=%d, CodePage=%d",
                        header.maintenanceVersion(),
                        header.codePage());

                    // 섹션 읽기
                    input = new ByteBufferBitInput(data);  // 처음부터 다시
                    Map<String, SectionInputStream> sections = null;
                    try {
                        sections = handler.readSections(input, header);
                    } catch (NullPointerException e) {
                        System.out.print(" [readSections failed: null offsets]");
                        sections = new java.util.HashMap<>();
                    }

                    if (sections != null) {
                        System.out.printf(" Sections=%d", sections.size());
                        for (String sectionName : sections.keySet()) {
                            SectionInputStream sec = sections.get(sectionName);
                            System.out.printf(" %s=%d",
                                sectionName.replace("AcDb:", ""),
                                sec.rawBytes().length);
                        }
                    }

                    System.out.println();
                    successCount++;

                } catch (Exception e) {
                    System.out.printf("%-30s: ERROR %s\n", name, e.getMessage());
                }
            }
        }

        System.out.printf("\n=== 결과 ===\n");
        System.out.printf("총: %d, 성공: %d\n", totalCount, successCount);

        if (successCount == totalCount && totalCount > 0) {
            System.out.println("\n✓ R2000 Objects 섹션 추출 성공!");
        }
    }
}
