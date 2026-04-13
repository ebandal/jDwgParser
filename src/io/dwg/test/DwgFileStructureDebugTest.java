package io.dwg.test;

import io.dwg.core.io.BitInput;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.version.DwgVersion;
import io.dwg.core.version.DwgVersionDetector;
import io.dwg.format.common.DwgFileStructureHandler;
import io.dwg.format.common.DwgFileStructureHandlerFactory;
import io.dwg.format.common.FileHeaderFields;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * DWG 파일 구조 분석 및 디버그 정보 출력
 */
public class DwgFileStructureDebugTest {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("사용법: java io.dwg.test.DwgFileStructureDebugTest <dwg_file_path>");
            System.out.println("예: java io.dwg.test.DwgFileStructureDebugTest samples/example_2004.dwg");
            return;
        }

        String filePath = args[0];

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  DWG 파일 구조 분석 & 디버그");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("파일: " + filePath + "\n");

        try {
            // 파일 읽기
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
            System.out.printf("파일 크기: %d bytes (0x%X)\n\n", fileBytes.length, fileBytes.length);

            // 버전 감지
            DwgVersion version = DwgVersionDetector.detect(fileBytes);
            System.out.printf("감지된 버전: %s (%s)\n", version.name(), version.versionString());

            // 파일 구조 핸들러
            DwgFileStructureHandler handler = DwgFileStructureHandlerFactory.forVersion(version);
            System.out.printf("파일 구조 핸들러: %s\n\n", handler.getClass().getSimpleName());

            // 원본 헤더 hex dump
            System.out.println("─────────────────────────────────────────────────────────────");
            System.out.println("원본 헤더 (첫 0x40 바이트):");
            System.out.println("─────────────────────────────────────────────────────────────");
            for (int i = 0; i < 0x40; i += 16) {
                System.out.printf("  %04X: ", i);
                for (int j = 0; j < 16 && (i + j) < 0x40; j++) {
                    System.out.printf("%02X ", fileBytes[i + j] & 0xFF);
                }
                System.out.println();
            }

            // 헤더 파싱
            BitInput input = new ByteBufferBitInput(fileBytes);
            FileHeaderFields header = handler.readHeader(input);

            System.out.println("\n─────────────────────────────────────────────────────────────");
            System.out.println("파싱된 헤더 필드:");
            System.out.println("─────────────────────────────────────────────────────────────");
            System.out.printf("  버전: %s\n", header.version());
            System.out.printf("  유지보수 버전: %d (0x%02X)\n", header.maintenanceVersion(), header.maintenanceVersion());
            System.out.printf("  코드페이지: %d (0x%04X)\n", header.codePage(), header.codePage());
            System.out.printf("  보안 플래그: 0x%08X\n", header.securityFlags());
            System.out.printf("  암호화 여부: %s\n", header.isEncrypted() ? "YES" : "NO");
            System.out.printf("  Preview Offset: 0x%08X (%d)\n", header.previewOffset(), header.previewOffset());
            System.out.printf("  Summary Info Offset: 0x%08X (%d)\n", header.summaryInfoOffset(), header.summaryInfoOffset());
            System.out.printf("  VBA Project Offset: 0x%016X (%d)\n", header.vbaProjectOffset(), header.vbaProjectOffset());

            // 버전별 디버그 정보
            debugVersionSpecificFields(header, version);

            // Section 읽기 시도
            System.out.println("\n─────────────────────────────────────────────────────────────");
            System.out.println("섹션 추출 시도:");
            System.out.println("─────────────────────────────────────────────────────────────");

            try {
                input = new ByteBufferBitInput(fileBytes);  // Reset
                handler.readHeader(input);

                var sections = handler.readSections(input, header);
                System.out.printf("✓ 섹션 추출 성공: %d개\n", sections.size());
                for (String sectionName : sections.keySet()) {
                    System.out.printf("  - %s\n", sectionName);
                }
            } catch (Exception e) {
                System.out.printf("✗ 섹션 추출 실패: %s\n", e.getMessage());
                System.out.printf("  원인: %s\n", e.getClass().getSimpleName());

                // 스택 트레이스 (제한)
                StackTraceElement[] stackTrace = e.getStackTrace();
                for (int i = 0; i < Math.min(3, stackTrace.length); i++) {
                    StackTraceElement ste = stackTrace[i];
                    System.out.printf("    at %s.%s (%s:%d)\n",
                        ste.getClassName(), ste.getMethodName(),
                        ste.getFileName(), ste.getLineNumber());
                }
            }

        } catch (Exception e) {
            System.out.printf("✗ 오류 발생: %s\n", e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
    }

    private static void debugVersionSpecificFields(FileHeaderFields header, DwgVersion version) {
        System.out.println("\n─────────────────────────────────────────────────────────────");
        System.out.println("버전별 필드 분석:");
        System.out.println("─────────────────────────────────────────────────────────────");

        if (version == DwgVersion.R2004) {
            System.out.println("  [R2004]");
            System.out.printf("    Summary Info Offset (section map ID): 0x%08X\n", header.summaryInfoOffset());
            System.out.printf("    VBA Project Offset (section map offset): 0x%016X\n", header.vbaProjectOffset());
            // Note: readHeader에서 0x6C에서 section map offset을 읽지만,
            // FileHeaderFields에 저장하는 필드가 제한적임
        } else if (version == DwgVersion.R2007) {
            System.out.println("  [R2007]");
            System.out.printf("    Summary Info Offset (page map offset): 0x%08X\n", header.summaryInfoOffset());
            System.out.printf("    VBA Project Offset (section map ID): 0x%016X\n", header.vbaProjectOffset());
        }
    }
}
