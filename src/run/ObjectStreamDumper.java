package run;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.BitStreamReader;
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
 * Object Stream 바이트 덤프 및 분석
 */
public class ObjectStreamDumper {
    public static void main(String[] args) throws Exception {
        // R2004 샘플 파일
        String[] files = {
            "samples/2004/Arc.dwg",
            "samples/2004/Circle.dwg"
        };

        for (String filePath : files) {
            dumpObjectStream(filePath);
        }
    }

    private static void dumpObjectStream(String filePath) throws Exception {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            System.out.println("파일 없음: " + path);
            return;
        }

        System.out.println("\n" + "=".repeat(100));
        System.out.println("분석: " + filePath);
        System.out.println("=".repeat(100));

        byte[] data = Files.readAllBytes(path);
        DwgVersion version = DwgVersionDetector.detect(data);
        DwgFileStructureHandler handler = DwgFileStructureHandlerFactory.forVersion(version);

        BitInput input = new ByteBufferBitInput(data);
        FileHeaderFields headerFields = handler.readHeader(input);

        input = new ByteBufferBitInput(data);
        Map<String, SectionInputStream> sections = handler.readSections(input, headerFields);

        SectionInputStream objectsStream = sections.get("AcDb:AcDbObjects");
        if (objectsStream == null) {
            System.out.println("❌ Objects 섹션 없음!");
            return;
        }

        byte[] objData = objectsStream.rawBytes();
        System.out.printf("Object Stream 크기: %d bytes\n\n", objData.length);

        // 처음 1000바이트 분석
        int analyzeLen = Math.min(1000, objData.length);

        // Hex dump
        System.out.println("HEX DUMP (처음 " + analyzeLen + " 바이트):");
        for (int i = 0; i < analyzeLen; i += 16) {
            System.out.printf("0x%04X: ", i);
            for (int j = 0; j < 16 && i + j < analyzeLen; j++) {
                System.out.printf("%02X ", objData[i + j] & 0xFF);
            }
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
        System.out.println("BitStream 단계별 분석:");

        // BitStreamReader를 사용해서 처음 20개의 기본 항목 파싱 시도
        ByteBufferBitInput buf = new ByteBufferBitInput(
            java.nio.ByteBuffer.wrap(objData, 0, analyzeLen));
        BitStreamReader reader = new BitStreamReader(buf, version);

        System.out.println("\n첫 20개 항목 (시도):");
        int itemCount = 0;
        while (!buf.isEof() && itemCount < 20) {
            try {
                // ModularShort 시도
                int val = reader.readModularShort();

                System.out.printf("[%2d] ModularShort=%d (0x%X)\n",
                    itemCount, val, val & 0xFFFF);

                itemCount++;

                if (itemCount >= 5) break;  // 처음 5개만

            } catch (Exception e) {
                System.out.printf("[%2d] Error: %s\n", itemCount, e.getMessage());
                break;
            }
        }

        System.out.println();
        System.out.println("Object 구조 패턴 분석:");
        analyzeObjectPatterns(objData);
    }

    private static void analyzeObjectPatterns(byte[] data) {
        System.out.println("가능한 ModularShort 값 찾기 (0-999 범위, 가능한 type code):");

        int found = 0;
        for (int offset = 0; offset < data.length - 1 && found < 10; offset++) {
            // Little-endian 16-bit 읽기 시뮬레이션
            int lowByte = data[offset] & 0xFF;
            int highByte = data[offset + 1] & 0xFF;

            // ModularShort 인코딩 분석 (단순화)
            int val = lowByte | (highByte << 8);

            if (val >= 1 && val <= 999) {
                // 추가 검증: 이전 바이트들이 합리적인 크기처럼 보이는지
                if (offset >= 2) {
                    int prevVal = (data[offset - 2] & 0xFF) | ((data[offset - 1] & 0xFF) << 8);
                    if (prevVal >= 1 && prevVal <= 500) {
                        System.out.printf("  offset 0x%04X: size=%d, type=%d\n",
                            offset - 2, prevVal, val);
                        found++;
                    }
                }
            }
        }
    }
}
