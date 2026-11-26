package io.dwg.core.io;

/**
 * The limits of one read, kept for the thread that reads: the bytes the reader may allocate for
 * section data in all, and the time it may take. The parts of the reader that allocate large
 * buffers from sizes found in the file call {@link #reserve}; the object loop calls
 * {@link #checkTime}. Without a current read (code that is not called by the reader) nothing is
 * checked.
 */
public final class ReadLimits {

    /** Thrown when a read goes past one of its limits. */
    public static final class LimitExceededException extends RuntimeException {
        LimitExceededException(String message) {
            super(message);
        }
    }

    private static final ThreadLocal<ReadLimits> CURRENT = new ThreadLocal<>();

    private final long maxBytes;
    private final long deadlineNanos;
    private long reserved;

    private ReadLimits(long maxBytes, long timeoutMillis) {
        this.maxBytes = maxBytes;
        this.deadlineNanos = timeoutMillis > 0 ? System.nanoTime() + timeoutMillis * 1_000_000L : Long.MAX_VALUE;
    }

    /** Starts the limits of a read on this thread; {@code timeoutMillis} 0 means no time limit. */
    public static void begin(long maxBytes, long timeoutMillis) {
        CURRENT.set(new ReadLimits(maxBytes, timeoutMillis));
    }

    /** Ends the read on this thread. */
    public static void end() {
        CURRENT.remove();
    }

    /**
     * Counts {@code bytes} against the read's allocation limit, before they are allocated.
     *
     * @throws LimitExceededException if the read would allocate more than its limit
     */
    public static void reserve(long bytes, String what) {
        ReadLimits limits = CURRENT.get();
        if (bytes < 0) {
            throw new IllegalStateException("negative size " + bytes + " for " + what);
        }
        if (limits == null) {
            if (bytes > Integer.MAX_VALUE - 8) {
                throw new IllegalStateException("size " + bytes + " for " + what + " is too large");
            }
            return;
        }
        if (bytes > Integer.MAX_VALUE - 8 || limits.reserved + bytes > limits.maxBytes) {
            throw new LimitExceededException(what + " needs " + bytes + " bytes, more than the read's limit of "
                    + limits.maxBytes + " bytes (" + limits.reserved + " already used)");
        }
        limits.reserved += bytes;
    }

    /**
     * @throws LimitExceededException if the read has taken longer than its time limit
     */
    public static void checkTime() {
        ReadLimits limits = CURRENT.get();
        if (limits != null && System.nanoTime() > limits.deadlineNanos) {
            throw new LimitExceededException("the read took longer than its time limit");
        }
    }
}
