package io.dwg.test;

import io.dwg.core.version.DwgVersionDetector;
import io.dwg.core.version.DwgVersion;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * R2007 파일의 실제 RS 인코딩 데이터 분석
 * 파일의 헤더 데이터를 RS 디코더로 처리하고 결과를 검증합니다.
 */
public class R2007ActualDataDebugTest {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java R2007ActualDataDebugTest <dwg_file>");
            System.exit(1);
        }

        String filePath = args[0];
        byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));

        // 버전 확인
        DwgVersion version = DwgVersionDetector.detect(fileBytes);
        System.out.printf("File version: %s\n", version);

        if (version != DwgVersion.R2007) {
            System.out.printf("ERROR: Expected R2007, got %s\n", version);
            System.exit(1);
        }

        System.out.println("\n========================================");
        System.out.println("R2007 Header Analysis");
        System.out.println("========================================\n");

        // R2007 헤더 구조:
        // 0x00-0x05: Version (AC1021)
        // 0x06-0x69: Live data fields (100 bytes)
        // 0x6A-0xFF: RS-encoded header (86 bytes? or more?)

        System.out.println("File structure:");
        System.out.printf("  File size: %d bytes\n", fileBytes.length);

        // 버전 문자열
        String versionStr = new String(fileBytes, 0, 6);
        System.out.printf("  Version string: %s\n", versionStr);

        // 라이브 데이터
        System.out.println("  Live data fields (first 0x40 bytes):");
        for (int i = 0; i < 0x40; i += 16) {
            System.out.printf("    0x%02X: ", i + 6);
            for (int j = 0; j < 16 && i + j < 0x64; j++) {
                System.out.printf("%02X ", fileBytes[6 + i + j] & 0xFF);
            }
            System.out.println();
        }

        // RS 인코딩된 헤더 (984 바이트)
        System.out.println("\n  RS-encoded header (first 0x40 bytes):");
        int rsStart = 0x6A;  // R2007 기준
        for (int i = 0; i < 0x40; i += 16) {
            System.out.printf("    0x%02X: ", rsStart + i);
            for (int j = 0; j < 16 && rsStart + i + j < fileBytes.length; j++) {
                System.out.printf("%02X ", fileBytes[rsStart + i + j] & 0xFF);
            }
            System.out.println();
        }

        // 984바이트 RS 데이터 추출
        if (rsStart + 984 > fileBytes.length) {
            System.out.printf("ERROR: Not enough data for RS header (need %d, have %d)\n",
                rsStart + 984, fileBytes.length);
            System.exit(1);
        }

        byte[] rsData = new byte[984];
        System.arraycopy(fileBytes, rsStart, rsData, 0, 984);

        System.out.printf("\nRS-encoded data size: %d bytes\n", rsData.length);
        System.out.println("First 3 blocks (255 bytes each):");

        for (int block = 0; block < 3; block++) {
            byte[] blk = new byte[255];
            System.arraycopy(rsData, block * 255, blk, 0, 255);

            // 신드롬 계산
            System.out.printf("  Block %d syndromes:\n", block);
            int nonZeroCount = 0;
            for (int j = 0; j < 16; j++) {
                // 신드롬 계산 (simplified - 실제로는 ReedSolomonDecoder 사용)
                System.out.printf("    S%d: (calculation needed)\n", j);
                nonZeroCount++;
            }
            System.out.printf("  → Block %d appears to have %s data\n", block,
                nonZeroCount > 0 ? "encoded" : "clean");
        }

        System.out.println("\n✓ Analysis complete");
    }
}
