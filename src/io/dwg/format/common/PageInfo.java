package io.dwg.format.common;

/**
 * 파일 내 데이터 페이지 위치와 크기 정보
 */
public class PageInfo {
    private long pageOffset;
    private long dataSize;
    private long pageId;

    public PageInfo(long pageOffset, long dataSize, long pageId) {
        this.pageOffset = pageOffset;
        this.dataSize = dataSize;
        this.pageId = pageId;
    }

    public long pageOffset() {
        return pageOffset;
    }

    public long dataSize() {
        return dataSize;
    }

    public long pageId() {
        return pageId;
    }

    @Override
    public String toString() {
        return String.format("PageInfo[id=%d, offset=%d, size=%d]", pageId, pageOffset, dataSize);
    }
}
