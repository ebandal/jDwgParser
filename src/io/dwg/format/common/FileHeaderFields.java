package io.dwg.format.common;

import io.dwg.core.version.DwgVersion;
import io.dwg.format.r13.R13SectionLocator;
import java.util.List;
import java.util.Map;

/**
 * 모든 버전에서 공통으로 추출하는 헤더 정보
 */
public class FileHeaderFields {
    private DwgVersion version;
    private int maintenanceVersion;
    private long previewOffset;
    private int codePage;
    private int securityFlags;
    private long summaryInfoOffset;
    private long vbaProjectOffset;
    private long sectionMapOffset;  // R2004 only
    private long pageMapOffset;     // R2007+ only
    private long sectionMapId;      // R2007+ only (page ID of section map)
    private Map<String, Long> sectionOffsets;
    private Map<String, Long> sectionSizes;
    private List<R13SectionLocator> sectionLocators;  // R13/R14 only

    public FileHeaderFields(DwgVersion version) {
        this.version = version;
        this.maintenanceVersion = 0;
        this.previewOffset = 0;
        this.codePage = 20127;  // US-ASCII
        this.securityFlags = 0;
        this.summaryInfoOffset = 0;
        this.vbaProjectOffset = 0;
    }

    public DwgVersion version() {
        return version;
    }

    public int maintenanceVersion() {
        return maintenanceVersion;
    }

    public void setMaintenanceVersion(int ver) {
        this.maintenanceVersion = ver;
    }

    public long previewOffset() {
        return previewOffset;
    }

    public void setPreviewOffset(long offset) {
        this.previewOffset = offset;
    }

    public int codePage() {
        return codePage;
    }

    public void setCodePage(int codePage) {
        this.codePage = codePage;
    }

    public int securityFlags() {
        return securityFlags;
    }

    public void setSecurityFlags(int flags) {
        this.securityFlags = flags;
    }

    public boolean isEncrypted() {
        return (securityFlags & 0x01) != 0;
    }

    public long summaryInfoOffset() {
        return summaryInfoOffset;
    }

    public void setSummaryInfoOffset(long offset) {
        this.summaryInfoOffset = offset;
    }

    public long vbaProjectOffset() {
        return vbaProjectOffset;
    }

    public void setVbaProjectOffset(long offset) {
        this.vbaProjectOffset = offset;
    }

    public long sectionMapOffset() {
        return sectionMapOffset;
    }

    public void setSectionMapOffset(long offset) {
        this.sectionMapOffset = offset;
    }

    public long pageMapOffset() {
        return pageMapOffset;
    }

    public void setPageMapOffset(long offset) {
        this.pageMapOffset = offset;
    }

    public long sectionMapId() {
        return sectionMapId;
    }

    public void setSectionMapId(long id) {
        this.sectionMapId = id;
    }

    public Map<String, Long> sectionOffsets() {
        return sectionOffsets;
    }

    public void setSectionOffsets(Map<String, Long> offsets) {
        this.sectionOffsets = offsets;
    }

    public Map<String, Long> sectionSizes() {
        return sectionSizes;
    }

    public void setSectionSizes(Map<String, Long> sizes) {
        this.sectionSizes = sizes;
    }

    public List<R13SectionLocator> sectionLocators() {
        return sectionLocators;
    }

    public void setSectionLocators(List<R13SectionLocator> locators) {
        this.sectionLocators = locators;
    }

    @Override
    public String toString() {
        return String.format("FileHeaderFields[%s, maintenance=%d, codePage=%d]",
            version, maintenanceVersion, codePage);
    }
}
