package io.dwg.core.exception;

/**
 * 쓰기 중 발생하는 checked 예외.
 */
public class DwgWriteException extends Exception {
    private long bitOffset = -1;

    public DwgWriteException(String message) {
        super(message);
    }

    public DwgWriteException(String message, Throwable cause) {
        super(message, cause);
    }

    public DwgWriteException(String message, long bitOffset) {
        super(message);
        this.bitOffset = bitOffset;
    }

    public DwgWriteException(String message, long bitOffset, Throwable cause) {
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
