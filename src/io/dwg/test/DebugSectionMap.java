package io.dwg.test;

import java.io.*;
import java.nio.ByteBuffer;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.format.r2007.R2007FileHeader;
import io.dwg.format.r2007.R2007PageMapParser;
import io.dwg.format.r2007.R2007SectionMapParser;
import io.dwg.format.r2007.R2007SystemPageReader;

public class DebugSectionMap {
    public static void main(String[] args) throws Exception {
        File f = new File("samples/2007/Arc.dwg");
        byte[] fileData = new byte[(int)f.length()];
        try (FileInputStream fis = new FileInputStream(f)) {
            fis.read(fileData);
        }
        
        R2007FileHeader h = R2007FileHeader.read(
            new ByteBufferBitInput(ByteBuffer.wrap(fileData)));
        
        byte[] pageMapDecompressed = R2007SystemPageReader.readSystemPage(
            new ByteBufferBitInput(ByteBuffer.wrap(fileData)),
            0x480L + h.pageMapOffset(), h.pageMapSizeComp(),
            h.pageMapSizeUncomp(), h.pageMapCorrection());
        
        var pageMap = R2007PageMapParser.parsePageMap(pageMapDecompressed);
        
        long cumul = 0, smOffset = -1;
        for (var entry : pageMap) {
            if (entry.pageId == h.sectionsMapId()) {
                smOffset = 0x480L + h.pageMapOffset() + cumul;
                break;
            }
            cumul += entry.size;
        }
        
        byte[] smDecompressed = R2007SystemPageReader.readSystemPage(
            new ByteBufferBitInput(ByteBuffer.wrap(fileData)),
            smOffset, h.sectionsMapSizeComp(),
            h.sectionsMapSizeUncomp(), h.sectionsMapCorrection());
        
        var sectionMap = R2007SectionMapParser.parseSectionMap(smDecompressed);
        
        System.out.println("=== SectionMap Contents ===\n");
        for (int i = 0; i < sectionMap.size(); i++) {
            var s = sectionMap.get(i);
            System.out.printf("Section %d: '%s' (size=%d, pages=%d)\n",
                i, s.sectionName, s.dataSize, s.numPages);
        }
    }
}
