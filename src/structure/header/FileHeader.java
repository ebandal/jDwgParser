package structure.header;

import java.util.List;

import structure.DwgVersion;
import structure.SectionLocator;
import structure.sectionpage.header.DataSectionPageHeader;
import structure.sectionpage.header.SystemSectionPageHeader;

public class FileHeader {
    public String versionId;
    public DwgVersion ver;
    
    public int imageSeeker;                         // R15
    
    public int previewSeeker;                       // R2004
    public byte appVer;                             // R2004
    public byte appMrVer;                           // R2004
    
    public short codePage;                          // R15, R2004
    
    // SECTION-LOCATOR RECORDS
    public List<SectionLocator> sectionLocatorList; // R15
    public short sectionLocatorCRC;
    
    public int security;                            // R15, R2004
    public int summarySeeker;                       // R15, R2004
    public int vbaSeeker;                           // R15, R2004
    
    public String fileIdstring;                     // R2004
    public int rootTreeNodeGap;                     // R2004
    public int lowermostLeftTreeNodeGap;            // R2004
    public int lowermostRightTreeNodeGap;           // R2004
    public int lastSectionPageId;                   // R2004
    public long lastSectionPageEndAddress;          // R2004
    public long secondHeaderDataAddress;            // R2004
    public int gapAmount;                           // R2004
    public int sectionPageAmount;                   // R2004
    public int sectionPageMapId;                    // R2004
    public long sectionPageMapAddress;              // R2004
    public int seciontMapId;                        // R2004
    public int sectionPageArraySize;                // R2004
    public int gapArraySize;                        // R2004
    public int crc32;                               // R2004
    
    public SystemSectionPageHeader ssph;            // R2004
    public DataSectionPageHeader dsph;              // R2004
}
