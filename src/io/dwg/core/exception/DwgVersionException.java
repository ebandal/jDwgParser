package io.dwg.core.exception;

/**
 * 지원하지 않는 버전의 DWG 파일을 읽을 때 발생.
 */
public class DwgVersionException extends RuntimeException {
    private String detectedVersion;

    public DwgVersionException(String versionString) {
        super("Unsupported DWG version: " + versionString);
        this.detectedVersion = versionString;
    }

    public String detectedVersion() {
        return detectedVersion;
    }
}
