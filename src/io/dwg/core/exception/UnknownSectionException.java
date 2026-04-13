package io.dwg.core.exception;

/**
 * 알 수 없는 섹션 이름 조회 시 발생.
 */
public class UnknownSectionException extends RuntimeException {
    private String sectionName;

    public UnknownSectionException(String sectionName) {
        super("Unknown DWG section: " + sectionName);
        this.sectionName = sectionName;
    }

    public String sectionName() {
        return sectionName;
    }
}
