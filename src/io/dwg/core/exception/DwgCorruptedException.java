package io.dwg.core.exception;

/**
 * 파일 손상(Sentinel 불일치, CRC 실패 등) 시 발생.
 */
public class DwgCorruptedException extends RuntimeException {
    private String context;
    private String detail;

    public DwgCorruptedException(String context, String detail) {
        super(context + ": " + detail);
        this.context = context;
        this.detail = detail;
    }

    public String context() {
        return context;
    }

    public String detail() {
        return detail;
    }
}
