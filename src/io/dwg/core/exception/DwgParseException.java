package io.dwg.core.exception;

/**
 * 파싱 중 발생하는 checked 예외.
 */
public class DwgParseException extends Exception {
    private long bitOffset = -1;

    public DwgParseException(String message) {
        super(message);
    }

    public DwgParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public DwgParseException(String message, long bitOffset) {
        super(message);
        this.bitOffset = bitOffset;
    }

    public DwgParseException(String message, long bitOffset, Throwable cause) {
        super(message, cause);
        this.bitOffset = bitOffset;
    }

    public long bitOffset() {
        return bitOffset;
    }

    @Override
    public String toString() {
        if (bitOffset >= 0) {
            return super.toString() + " [bitOffset=" + bitOffset + "]";
        }
        return super.toString();
    }
}
