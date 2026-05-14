package run;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import java.nio.file.Paths;

/**
 * R2007 객체 추출 간단한 테스트
 * RS 디코더 fix 검증
 */
public class SimpleR2007Test {
    public static void main(String[] args) throws Exception {
        String[] files = {
            "samples/2007/Arc.dwg",
            "samples/2007/Line.dwg",
            "samples/2007/Circle.dwg",
            "samples/2007/Text.dwg"
        };

        int successCount = 0;
        int failCount = 0;

        System.out.println("=== R2007 파일 파싱 테스트 ===\n");

        for (String file : files) {
            try {
                System.out.printf("%-35s: ", file);
                DwgDocument doc = DwgReader.defaultReader().open(Paths.get(file));
                System.out.printf("Version=%s Objects=%d\n", doc.version(), doc.objectMap().size());

                if (doc.objectMap().isEmpty()) {
                    System.out.println("  ⚠ 객체가 없습니다");
                    failCount++;
                } else {
                    successCount++;
                }
            } catch (Exception e) {
                System.out.printf("❌ %s: %s\n", file, e.getMessage());
                failCount++;
            }
        }

        System.out.printf("\n=== 결과 ===\n");
        System.out.printf("성공: %d, 실패: %d\n", successCount, failCount);

        if (failCount == 0 && successCount > 0) {
            System.out.println("✅ R2007 파싱 정상!");
        }
    }
}
