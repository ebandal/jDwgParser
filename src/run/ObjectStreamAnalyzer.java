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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * R2004 Object Stream 구조 분석 도구
 */
public class ObjectStreamAnalyzer {
    public static void main(String[] args) throws Exception {
        String[] files = {
            "samples/example_2004.dwg",
            "samples/Arc_2004.dwg",
            "samples/Circle_2004.dwg"
        };

        for (String filePath : files) {
            analyzeFile(filePath);
        }
    }

    private static void analyzeFile(String filePath) throws Exception {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            System.out.println("파일 없음: " + path);
            return;
        }

        System.out.println("\n" + "=".repeat(100));
        System.out.println("분석: " + filePath);
        System.out.println("=".repeat(100));

        byte[] data = Files.readAllBytes(path);

        // DwgReader를 사용하면 섹션이 제대로 추출됨
        DwgDocument doc = null;
        try {
            doc = DwgReader.defaultReader().open(data);
        } catch (Exception e) {
            System.out.println("Document load error (expected - may have zero objects): " + e.getMessage());
        }

        DwgVersion version = DwgVersionDetector.detect(data);
        DwgFileStructureHandler handler = DwgFileStructureHandlerFactory.forVersion(version);

        BitInput input = new ByteBufferBitInput(data);
        FileHeaderFields headerFields = handler.readHeader(input);

        input = new ByteBufferBitInput(data);
        Map<String, SectionInputStream> sections = handler.readSections(input, headerFields);

        System.out.println("사용 가능한 섹션: " + sections.keySet());

        SectionInputStream objectsStream = sections.get("AcDb:AcDbObjects");
        if (objectsStream == null) {
            System.out.println("❌ Objects 섹션 없음!");
            return;
        }

        byte[] objData = objectsStream.rawBytes();
        System.out.printf("Object Stream 크기: %d bytes (0x%X)\n", objData.length, objData.length);
        System.out.println();

        // 처음 500바이트를 분석
        int analyzeLen = Math.min(500, objData.length);
        System.out.println("첫 " + analyzeLen + "바이트 분석:");
        System.out.println();

        // Hex dump
        for (int i = 0; i < analyzeLen; i += 16) {
            System.out.printf("0x%04X: ", i);

            // Hex
            for (int j = 0; j < 16 && i + j < analyzeLen; j++) {
                System.out.printf("%02X ", objData[i + j] & 0xFF);
            }

            // ASCII
            System.out.print("  |  ");
            for (int j = 0; j < 16 && i + j < analyzeLen; j++) {
                byte b = objData[i + j];
                if (b >= 32 && b < 127) {
                    System.out.print((char) b);
                } else {
                    System.out.print(".");
                }
            }
            System.out.println("|");
        }

        System.out.println();
        System.out.println("BitStream 분석:");

        // 처음 100바이트를 BitStreamReader로 분석
        ByteBufferBitInput buf = new ByteBufferBitInput(
            java.nio.ByteBuffer.wrap(objData, 0, Math.min(100, objData.length)));
        io.dwg.core.io.BitStreamReader reader = new io.dwg.core.io.BitStreamReader(buf, version);

        System.out.println("첫 10개 항목:");
        for (int i = 0; i < 10; i++) {
            try {
                if (buf.isEof()) {
                    System.out.println("  EOF reached");
                    break;
                }

                // ModularShort 시도
                int val = reader.readModularShort();
                System.out.printf("  [%d] ModularShort: %d (0x%X)\n", i, val, val & 0xFFFF);

                if (buf.isEof()) break;
            } catch (Exception e) {
                System.out.printf("  [%d] Error: %s\n", i, e.getMessage());
                break;
            }
        }

        System.out.println();
        System.out.println("패턴 검색:");

        // 일반적인 object 마커나 패턴 검색
        searchPatterns(objData);
    }

    private static void searchPatterns(byte[] data) {
        // 1. ModularShort 가능성 있는 값들 (1-1000 범위의 type codes)
        System.out.println("가능한 object type codes (1-999):");
        int found = 0;
        for (int offset = 0; offset < data.length - 1 && found < 20; offset++) {
            int val = ((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8));
            if (val >= 1 && val <= 999) {
                // 가능한 type code
                // 추가 검증: 이전 바이트들이 object size처럼 보이는지
                System.out.printf("  offset 0x%04X: value=%d\n", offset, val);
                found++;
            }
        }

        System.out.println();
        System.out.println("일반 텍스트 마커:");
        // "AcDb" 같은 마커 검색
        String target = "AcDb";
        byte[] targetBytes = target.getBytes();
        for (int i = 0; i < data.length - targetBytes.length; i++) {
            boolean found2 = true;
            for (int j = 0; j < targetBytes.length; j++) {
                if (data[i + j] != targetBytes[j]) {
                    found2 = false;
                    break;
                }
            }
            if (found2) {
                System.out.printf("  Found '%s' at offset 0x%04X\n", target, i);
                // 다음 문자열도 출력
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < 50 && i + targetBytes.length + j < data.length; j++) {
                    byte b = data[i + targetBytes.length + j];
                    if (b >= 32 && b < 127) {
                        sb.append((char) b);
                    } else if (b == 0) {
                        break;
                    } else {
                        break;
                    }
                }
                if (sb.length() > 0) {
                    System.out.printf("    Next string: '%s'\n", sb);
                }
            }
        }
    }
}
