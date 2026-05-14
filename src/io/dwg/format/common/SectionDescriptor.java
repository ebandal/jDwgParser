package io.dwg.format.common;

/**
 * R2004+ 섹션 맵 내의 각 섹션 기술자 정보
 */
public class SectionDescriptor {
    private String name;
    private long offset;  // R2004: byte offset in file
    private long compressedSize;
    private long uncompressedSize;
    private int compressionType;  // 0=none, 2=LZ77
    private java.util.List<PageInfo> pages;

    public SectionDescriptor(String name) {
        this.name = name;
        this.pages = new java.util.ArrayList<>();
    }

    public String name() {
        return name;
    }

    public long offset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long compressedSize() {
        return compressedSize;
    }

    public void setCompressedSize(long size) {
        this.compressedSize = size;
    }

    public long uncompressedSize() {
        return uncompressedSize;
    }

    public void setUncompressedSize(long size) {
        this.uncompressedSize = size;
    }

    public int compressionType() {
        return compressionType;
    }

    public void setCompressionType(int type) {
        this.compressionType = type;
    }

    public java.util.List<PageInfo> pages() {
        return pages;
    }

    public void addPage(PageInfo page) {
        pages.add(page);
    }

    @Override
    public String toString() {
        return String.format("SectionDescriptor[%s, compressed=%d, uncompressed=%d, pages=%d]",
            name, compressedSize, uncompressedSize, pages.size());
    }
}
