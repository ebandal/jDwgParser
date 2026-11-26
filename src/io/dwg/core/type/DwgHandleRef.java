package io.dwg.core.type;

/**
 * Handle 참조를 나타내는 값 객체.
 * 지연 해석(lazy resolve)을 위해 실제 객체 대신 핸들만 보관.
 */
public final class DwgHandleRef {
    /** The null handle (0). */
    public static final DwgHandleRef NULL = new DwgHandleRef(0);

    private final long rawHandle;

    /** Recently made references, by handle; a slot holds the last one that fell in it. */
    private static final DwgHandleRef[] RECENT = new DwgHandleRef[4096];

    /**
     * A reference to {@code handle}: the same instance as one made recently for the same handle
     * (layers, owners and line types are referred to over and over), or a new one. The cache has
     * a fixed size, so it does not grow with the drawing; the reader makes its references with
     * this.
     */
    public static DwgHandleRef of(long handle) {
        if (handle == 0) {
            return NULL;
        }
        int slot = (int) (handle ^ (handle >>> 32)) & (RECENT.length - 1);
        DwgHandleRef r = RECENT[slot];
        if (r != null && r.rawHandle == handle) {
            return r;
        }
        r = new DwgHandleRef(handle);
        RECENT[slot] = r;                       // a race only costs sharing: the reference is immutable
        return r;
    }

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
