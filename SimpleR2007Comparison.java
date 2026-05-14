import java.io.*;
import java.nio.file.*;
import io.dwg.core.util.ReedSolomonDecoder;

/**
 * R2004 vs R2007 비교 - RS 디코더 차이점만 테스트
 * Build system 복잡도를 피하고 핵심만 검증
 */
public class SimpleR2007Comparison {
    static long readLE64(byte[] data, int off) {
        long result = 0;
        for (int i = 0; i < 8 && off + i < data.length; i++) {
            result |= ((long)(data[off + i] & 0xFF)) << (i * 8);
        }
        return result;
    }

    static String getVersionString(byte[] data) {
        if (data.length < 6) return "???";
        return new String(data, 0, 6).trim();
    }

    public static void main(String[] args) throws IOException {
        System.out.println("=== R2004 vs R2007 헤더 비교 ===\n");

        // R2004 테스트
        System.out.println("【 R2004 파일 】");
        testFile("samples/2004/Arc.dwg", "R2004");

        System.out.println("\n【 R2007 파일 】");
        testFile("samples/2007/Arc.dwg", "R2007");
    }

    static void testFile(String filePath, String version) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            System.out.printf("%s: 파일 없음\n", filePath);
            return;
        }

        byte[] data = Files.readAllBytes(path);
        System.out.printf("파일: %s\n", filePath);
        System.out.printf("크기: %,d bytes\n", data.length);
        System.out.printf("버전: %s\n", getVersionString(data));

        // 공통: 0x20에서 page map offset 읽기
        if (data.length >= 0x28) {
            long pageMapOff = readLE64(data, 0x20);
            System.out.printf("Page Map Offset (0x20): 0x%X\n", pageMapOff);
        }

        // R2007만: RS 디코더 검증
        if ("R2007".equals(version)) {
            if (data.length >= 0x80 + 0x3d8) {
                byte[] rsEncoded = new byte[0x3d8];
                System.arraycopy(data, 0x80, rsEncoded, 0, 0x3d8);

                byte[] decoded = ReedSolomonDecoder.decodeR2007Data(rsEncoded);
                if (decoded != null && decoded.length == 717) {
                    System.out.printf("RS 디코더: ✅ %d bytes\n", decoded.length);

                    // 디코드된 헤더에서도 page map 확인
                    if (decoded.length >= 0x28) {
                        long pageMapOffRS = readLE64(decoded, 0x20);
                        System.out.printf("Page Map Offset (RS): 0x%X\n", pageMapOffRS);
                    }
                } else {
                    System.out.printf("RS 디코더: ❌\n");
                }
            }
        }
    }
}
