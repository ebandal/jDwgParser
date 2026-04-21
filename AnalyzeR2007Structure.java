import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import io.dwg.core.util.ReedSolomonDecoder;

/**
 * R2007 파일 구조 분석 - RS 디코더 검증
 */
public class AnalyzeR2007Structure {
    static long readLE64(byte[] data, int off) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result |= ((long)(data[off + i] & 0xFF)) << (i * 8);
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        System.out.println("=== R2007 파일 RS 디코더 검증 ===\n");

        String[] files = {
            "samples/2007/Arc.dwg",
            "samples/2007/Line.dwg",
            "samples/2007/Circle.dwg",
            "samples/2007/Text.dwg"
        };

        int decodeCount = 0;
        int totalCount = 0;

        for (String filePath : files) {
            try {
                Path path = Paths.get(filePath);
                if (!Files.exists(path)) continue;

                byte[] data = Files.readAllBytes(path);
                totalCount++;

                System.out.printf("%-40s: ", filePath);

                // RS 인코딩 데이터
                if (data.length >= 0x80 + 0x3d8) {
                    byte[] rsEncoded = new byte[0x3d8];
                    System.arraycopy(data, 0x80, rsEncoded, 0, 0x3d8);

                    byte[] decoded = ReedSolomonDecoder.decodeR2007Data(rsEncoded);
                    if (decoded != null && decoded.length == 717) {
                        System.out.printf("✅ %d bytes\n", decoded.length);
                        decodeCount++;

                        // 페이지 맵 오프셋 확인
                        if (decoded.length >= 0x20 + 8) {
                            long pageMapOff = readLE64(decoded, 0x20);
                            System.out.printf("  → Page Map at 0x%X\n", pageMapOff);
                        }
                    } else {
                        System.out.printf("❌ null or wrong size\n");
                    }
                } else {
                    System.out.printf("⚠ 파일 너무 작음\n");
                }

            } catch (Exception e) {
                System.out.printf("❌ %s\n", e.getMessage());
            }
        }

        System.out.printf("\n=== 결과 ===\n");
        System.out.printf("성공: %d / %d\n", decodeCount, totalCount);
        if (decodeCount == totalCount && totalCount > 0) {
            System.out.println("✅ R2007 RS 디코더 완벽 작동!");
        }
    }
}
