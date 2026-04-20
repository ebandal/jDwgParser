package structure.entities;

public class DwgHandleRef {
    private long rawHandle;

    public DwgHandleRef(long rawHandle) {
        this.rawHandle = rawHandle;
    }

    public long rawHandle() {
        return rawHandle;
    }

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
