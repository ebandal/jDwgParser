package io.dwg.core.type;

/**
 * Handle 참조를 나타내는 값 객체.
 * 지연 해석(lazy resolve)을 위해 실제 객체 대신 핸들만 보관.
 */
public class DwgHandleRef {
    private long rawHandle;

    public DwgHandleRef(long rawHandle) {
        this.rawHandle = rawHandle;
    }

    /**
     * 저장된 핸들 값 반환
     */
    public long rawHandle() {
        return rawHandle;
    }

    /**
     * Null 핸들인지 확인
     */
    public boolean isNull() {
        return rawHandle == 0;
    }

    @Override
    public String toString() {
        return String.format("HandleRef[0x%X]", rawHandle);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DwgHandleRef)) return false;
        DwgHandleRef that = (DwgHandleRef) o;
        return rawHandle == that.rawHandle;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(rawHandle);
    }
}
