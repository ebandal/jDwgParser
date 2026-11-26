package io.dwg.sections.handles;

/**
 * 핸들과 파일 오프셋의 쌍.
 */
public final class HandleEntry {
    private final long handle;
    private final long offset;

    public HandleEntry(long handle, long offset) {
        this.handle = handle;
        this.offset = offset;
    }

    public long handle() {
        return handle;
    }

    public long offset() {
        return offset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HandleEntry)) return false;
        HandleEntry other = (HandleEntry) o;
        return handle == other.handle
                && offset == other.offset;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(handle, offset);
    }

    @Override
    public String toString() {
        return "HandleEntry[" + "handle=" + handle + ", " + "offset=" + offset + "]";
    }
}
