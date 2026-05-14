package run;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;
import java.nio.file.Paths;

/**
 * R2004 객체 추출 간단한 테스트
 */
public class SimpleR2004Test {
    public static void main(String[] args) throws Exception {
        String[] files = {
            "samples/2004/Arc.dwg",
            "samples/2004/Line.dwg",
            "samples/example_2004.dwg"
        };

        for (String file : files) {
            try {
                System.out.println("\n테스트: " + file);
                DwgDocument doc = DwgReader.defaultReader().open(Paths.get(file));
                System.out.println("  버전: " + doc.version());
                System.out.println("  객체: " + doc.objectMap().size());

                if (doc.objectMap().isEmpty()) {
                    System.out.println("  ⚠ 객체가 없습니다");
                }
            } catch (Exception e) {
                System.out.println("  ❌ " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }
}
