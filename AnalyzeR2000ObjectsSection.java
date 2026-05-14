import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * R2000 Objects 섹션 상세 분석
 * Header 이후 데이터 구조 파악
 */
public class AnalyzeR2000ObjectsSection {
    public static void main(String[] args) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("samples/2000/Arc.dwg"));

        System.out.println("=== R2000 Objects Section Analysis ===\n");

        int headerEnd = 0x6D91;    // 28049
        int firstSentinel = 0x8B9D; // 35741
        int dataSize = firstSentinel - headerEnd;

        System.out.printf("Header End: 0x%X\n", headerEnd);
        System.out.printf("First Sentinel: 0x%X\n", firstSentinel);
        System.out.printf("Data between: %d bytes (0x%X)\n\n", dataSize, dataSize);

        // 섹션 데이터 추출
        byte[] objectsData = new byte[dataSize];
        System.arraycopy(data, headerEnd, objectsData, 0, dataSize);

        // 섹션 내에서 패턴 찾기
        System.out.println("【 Data Structure Analysis 】");

        // 1. 처음 부분 분석 (클래스 정보)
        System.out.println("\n1. Classes/Dictionary Information (first 200 bytes):");
        System.out.print("   Text: ");
        for (int i = 0; i < Math.min(200, objectsData.length); i++) {
            byte b = objectsData[i];
            if (b >= 32 && b < 127) {
                System.out.print((char)b);
            } else if (b == 0) {
                System.out.print("·");  // null terminator
            } else {
                System.out.print(".");
            }
        }
        System.out.println();

        // 2. Null 바운더리 찾기
        System.out.println("\n2. Null-terminated Strings (Classes?):");
        int pos = 0;
        int stringCount = 0;
        StringBuilder currentString = new StringBuilder();
        for (int i = 0; i < Math.min(500, objectsData.length) && stringCount < 20; i++) {
            byte b = objectsData[i];
            if (b == 0) {
                if (currentString.length() > 0) {
                    String str = currentString.toString();
                    if (str.startsWith("AcDb") || str.length() > 3) {
                        System.out.printf("   [%3d] @ 0x%X: \"%s\"\n", stringCount, i - currentString.length(), str);
                        stringCount++;
                    }
                    currentString = new StringBuilder();
                }
            } else if (b >= 32 && b < 127) {
                currentString.append((char)b);
            }
        }

        // 3. 패턴 분석 (고정 크기 구조?)
        System.out.println("\n3. Potential Fixed-Size Records:");
        System.out.println("   Looking for repeating patterns...");

        // 100개의 서로 다른 오프셋에서 32바이트씩 확인
        System.out.println("   Sample 32-byte chunks (every 256 bytes):");
        for (int i = 0; i < Math.min(objectsData.length, 3000); i += 256) {
            System.out.printf("   0x%X: ", i);
            for (int j = 0; j < Math.min(32, objectsData.length - i); j++) {
                byte b = objectsData[i + j];
                if (b >= 32 && b < 127) {
                    System.out.print((char)b);
                } else {
                    System.out.print(String.format("%02X", b & 0xFF));
                }
            }
            System.out.println();
        }

        // 4. 센티넬 근처 분석
        System.out.println("\n4. Data Near First Sentinel (0x8B9D):");
        int sentinelOffset = dataSize - 100;  // 센티넬 100바이트 전
        if (sentinelOffset > 0) {
            System.out.println("   Last 100 bytes before sentinel:");
            System.out.print("   Text: ");
            for (int i = sentinelOffset; i < dataSize && i < sentinelOffset + 100; i++) {
                byte b = objectsData[i];
                if (b >= 32 && b < 127) {
                    System.out.print((char)b);
                } else {
                    System.out.print(".");
                }
            }
            System.out.println();
        }

        // 5. 이진 패턴 분석
        System.out.println("\n5. Binary Pattern Analysis:");
        System.out.println("   Looking for length fields or handles...");

        // 처음 50바이트를 LE32로 해석
        System.out.println("   Offsets 0-50 (interpreted as LE32):");
        for (int i = 0; i < Math.min(50, objectsData.length - 4); i += 4) {
            int value = readLE32(objectsData, i);
            if (value > 0 && value < 0x10000) {  // 합리적인 범위
                System.out.printf("   0x%X: 0x%X (%d)\n", i, value, value);
            }
        }

        // 구조 추측
        System.out.println("\n【 Structure Hypothesis 】");
        System.out.println("R2000 Objects section appears to contain:");
        System.out.println("1. Class definitions (AcDbDictionary, etc.)");
        System.out.println("2. Handle mappings");
        System.out.println("3. Entity data");
        System.out.println("4. All interleaved without clear section boundaries");
        System.out.println("\nApproach for extraction:");
        System.out.println("- Use null-terminated strings to identify classes");
        System.out.println("- Use sentinel markers (0x00×16) as alignment guides");
        System.out.println("- Parse sequentially like a stream");
    }

    static int readLE32(byte[] data, int off) {
        return (data[off] & 0xFF)
             | ((data[off + 1] & 0xFF) << 8)
             | ((data[off + 2] & 0xFF) << 16)
             | ((data[off + 3] & 0xFF) << 24);
    }
}
