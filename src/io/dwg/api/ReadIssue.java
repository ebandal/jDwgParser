package io.dwg.api;

/**
 * Something the reader could not read as it should, but went on past. The document is still
 * returned; the issues say which parts of it are missing or may be incomplete.
 */
public final class ReadIssue {
    private final Kind kind;
    private final long handle;
    private final String objectType;
    private final String message;

    public ReadIssue(Kind kind, long handle, String objectType, String message) {
        this.kind = kind;
        this.handle = handle;
        this.objectType = objectType;
        this.message = message;
    }

    /** What went wrong. */
    public Kind kind() {
        return kind;
    }

    /** The handle of the object concerned, or -1 when the issue is not about one object. */
    public long handle() {
        return handle;
    }

    /** The type name of that object, or {@code null}. */
    public String objectType() {
        return objectType;
    }

    /** A description for people, not meant to be parsed. */
    public String message() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReadIssue)) return false;
        ReadIssue other = (ReadIssue) o;
        return java.util.Objects.equals(kind, other.kind)
                && handle == other.handle
                && java.util.Objects.equals(objectType, other.objectType)
                && java.util.Objects.equals(message, other.message);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(kind, handle, objectType, message);
    }


    public enum Kind {
        /** A section, or a part of one, could not be read; what it holds may be missing. */
        SECTION,
        /** An object could not be read at all and is not in the document. */
        OBJECT_SKIPPED,
        /**
         * The data of an object could not be read to its end, or its fields did not end where the
         * data does; some of its fields may be missing or wrong.
         */
        OBJECT_DATA,
        /**
         * The handles of an object (owner, reactors, layer, ...) could not be read to their end,
         * or some were left unread.
         */
        OBJECT_HANDLES
    }

    /** An issue that is not about one object. */
    public static ReadIssue section(String message) {
        return new ReadIssue(Kind.SECTION, -1, null, message);
    }

    @Override
    public String toString() {
        return kind + (handle >= 0 ? " " + Long.toHexString(handle).toUpperCase() : "")
                + (objectType != null ? " " + objectType : "") + ": " + message;
    }
}
