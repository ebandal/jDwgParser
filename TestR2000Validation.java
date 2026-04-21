import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * R2000 (AC1015) 파일 검증 테스트
 * 15개의 R2000 샘플 파일을 확인하고 구조를 분석
 */
public class TestR2000Validation {
    static long readLE64(byte[] data, int off) {
        long result = 0;
        for (int i = 0; i < 8 && off + i < data.length; i++) {
            result |= ((long)(data[off + i] & 0xFF)) << (i * 8);
        }
        return result;
    }

    static int readLE32(byte[] data, int off) {
        int result = 0;
        for (int i = 0; i < 4 && off + i < data.length; i++) {
            result |= ((data[off + i] & 0xFF)) << (i * 8);
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        System.out.println("=== R2000 (AC1015) 파일 검증 ===\n");

        int totalCount = 0;
        int validCount = 0;

        try (Stream<Path> paths = Files.walk(Paths.get("samples/2000"))) {
            var results = paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".dwg"))
                .sorted()
                .toList();

            System.out.printf("발견된 R2000 파일: %d개\n\n", results.size());

            for (Path path : results) {
                totalCount++;
                String name = path.getFileName().toString();

                try {
                    byte[] data = Files.readAllBytes(path);

                    // 버전 확인
                    String sig = data.length >= 6 ? new String(data, 0, 6) : "???";

                    if (!"AC1015".equals(sig)) {
                        System.out.printf("%-35s: ⚠️  버전 불일치 (%s)\n", name, sig);
                        continue;
                    }

                    System.out.printf("%-35s: ✅", name);
                    System.out.printf(" Size=%,d", data.length);

                    // R2000 헤더 기본 정보 추출 (R13과 유사한 구조)
                    // 0x00-0x05: Version (AC1015)
                    // 0x06-0x0B: Unknown
                    // 이후: Section locators

                    if (data.length > 0x20) {
                        // 간단한 특징 분석
                        int maybeOffsets = readLE32(data, 0x20);
                        System.out.printf(" Offset@0x20=0x%X", maybeOffsets);
                    }

                    System.out.println();
                    validCount++;

                } catch (Exception e) {
                    System.out.printf("%-35s: ❌ %s\n", name, e.getMessage());
                }
            }
        }

        System.out.printf("\n=== 결과 ===\n");
        System.out.printf("총: %d, 유효: %d, 실패: %d\n", totalCount, validCount, totalCount - validCount);

        if (validCount == totalCount && totalCount > 0) {
            System.out.println("\n✅ 모든 R2000 파일이 유효한 DWG 형식입니다");
            System.out.println("다음 단계: R2000FileStructureHandler로 전체 파싱 테스트");
        }
    }
}
