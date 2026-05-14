package io.dwg.core.version;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import io.dwg.core.exception.DwgVersionException;

/**
 * 파일 첫 6바이트를 읽어 DwgVersion을 판별하는 유틸리티.
 */
public class DwgVersionDetector {

    /**
     * 헤더 바이트 배열에서 DWG 버전 감지
     */
    public static DwgVersion detect(byte[] headerBytes) {
        if (headerBytes == null || headerBytes.length < 6) {
            throw new DwgVersionException("Invalid header length");
        }

        // 첫 6바이트를 ASCII 문자열로 읽기
        String versionStr = new String(headerBytes, 0, 6, StandardCharsets.US_ASCII);
        return DwgVersion.fromString(versionStr);
    }

    /**
     * 파일 경로에서 DWG 버전 감지
     */
    public static DwgVersion detect(Path filePath) throws Exception {
        byte[] headerBytes = Files.readAllBytes(filePath);
        if (headerBytes.length < 6) {
            throw new DwgVersionException("File too short to detect version");
        }
        return detect(headerBytes);
    }

    /**
     * DWG 파일 여부 확인 (AC10 로 시작)
     */
    public static boolean isDwgFile(byte[] headerBytes) {
        if (headerBytes == null || headerBytes.length < 4) {
            return false;
        }
        String header = new String(headerBytes, 0, 4, StandardCharsets.US_ASCII);
        return header.startsWith("AC10");
    }
}
