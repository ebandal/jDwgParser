package io.dwg.core.type;

/**
 * DWG 객체 핸들. 파일 내 모든 객체의 고유 식별자.
 */
public class DwgHandle {
    private int code;
    private long value;

    public DwgHandle(int code, long value) {
        this.code = code;
        this.value = value;
    }

    /**
     * 절대 핸들 생성
     */
    public static DwgHandle absolute(long value) {
        return new DwgHandle(0, value);
    }

    public long value() {
        return value;
    }

    public int code() {
        return code;
    }

    /**
     * 절대 핸들인지 확인
     */
    public boolean isAbsolute() {
        return code == 0;
    }

    /**
     * code 기반으로 실제 절대 핸들 계산
     */
    public long resolve(long ownerHandle) {
        switch (code) {
            case 0: return value;  // absolute
            case 1: return ownerHandle + value;  // relative to owner
            case 2: return ownerHandle - value;  // relative negative
            case 4: return value;  // previous owned
            case 8: return value;  // next owned
            default: return value;
        }
    }

    @Override
    public String toString() {
        return String.format("Handle[code=%d, value=0x%X]", code, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DwgHandle)) return false;
        DwgHandle that = (DwgHandle) o;
        return code == that.code && value == that.value;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(((long)code << 56) | value);
    }
}
