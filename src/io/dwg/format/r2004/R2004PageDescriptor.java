package io.dwg.format.r2004;

import io.dwg.core.io.BitInput;

/**
 * 페이지 위치와 크기.
 */
public class R2004PageDescriptor {
    private final long pageId;
    private final long dataSize;
    private final long pageOffset;

    public R2004PageDescriptor(long pageId, long dataSize, long pageOffset) {
        this.pageId = pageId;
        this.dataSize = dataSize;
        this.pageOffset = pageOffset;
    }

    public static R2004PageDescriptor read(BitInput input) {
        long pageId     = input.readRawLong() & 0xFFFFFFFFL;
        long dataSize   = input.readRawLong() & 0xFFFFFFFFL;
        long pageOffset = input.readRawLong() & 0xFFFFFFFFL;
        return new R2004PageDescriptor(pageId, dataSize, pageOffset);
    }

    public long pageId()     { return pageId; }
    public long dataSize()   { return dataSize; }
    public long pageOffset() { return pageOffset; }
}
