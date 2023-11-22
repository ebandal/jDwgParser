package decode;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;
import java.util.ArrayList;

import structure.Dwg;
import structure.SectionLocator;
import structure.header.FileHeader;

public class DecoderR14 {
    public static final Logger log = Logger.getLogger(DecoderR14.class.toString());
    
    public static int readFileHeader(RandomAccessFile raf, int off, FileHeader fileHeader) throws IOException {
        int offset = off;
        
        byte[] buf = new byte[25];
        int readLen = raf.read(buf, offset, 25-6);
        
        // in R14, 5 0's and the ACADMAINTVER variable
        offset += 6;
        
        // and a byte of 1
        offset += 1;
        
        // IMAGE SEEKER
        // at 0x0D is a seeker(4byte long absolute address) for the beginning sentinel of the image data
        fileHeader.imageSeeker = ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        offset += 4;
        log.finest("Image Skeer: " + fileHeader.imageSeeker);
        
        // OBJECT FREE SPACE
        
        // TEMPLATE (optional)
        
        // DWGCODEPAGE
        // bytes at 0x13 and 0x14 are a raw short indicating the value of the code page for this drawing file
        offset = 0x13;
        
        fileHeader.codePage = ByteBuffer.wrap(buf, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        offset += 2;
        log.finest("DWG Code Page : " + fileHeader.codePage);
        
        // SECTION-LOCATOR RECORDS
        // at 0x15 is a long that tell how many sets of recno/seeker/length records follow.
        int recordsNum = ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        offset += 4;
        
        fileHeader.sectionLocatorList = new ArrayList<SectionLocator>();
        log.finest("number of Records Set " + recordsNum);
        
        short calCRC = 0;
        buf = new byte[25 + 9*recordsNum + 2 + 16];
        readLen = raf.read(buf, offset, 9*recordsNum + 2+ 16);
        
        for (int i=0; i<recordsNum; i++) {
            SectionLocator sl = new SectionLocator();
            
            sl.number = buf[offset];
            offset += 1;
            
            sl.seeker = ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            offset += 4;
            
            sl.size = ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            offset += 4;
            
            log.finest("Record Number : " + sl.number);
            fileHeader.sectionLocatorList.add(sl);
            
            calCRC = calculateCRC(calCRC, recordsNum);
        }
        
        short crc = ByteBuffer.wrap(buf, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        offset += 2;
        /*
        if (crc != calCRC) {
            throw new DwgCrcMismatchException();
        }
        */
        
        // 16bytes appears after CRC
        // 0x95,0xA0,0x4E,0x28,0x99,0x82,0x1A,0xE5,0x41,0xE0,0x5F,0x9D,0x3A,0x4D,0x00
        offset += 16;
        
        return offset - off;
    }
    
    public static int readData(RandomAccessFile raf, Dwg dwg) throws IOException, DwgParseException {
        byte[] buf = null;
        int offset = 0;
        
        for (SectionLocator sl: dwg.header.sectionLocatorList) {
            switch(sl.number) {
            case 0:
                // header variable
                buf = new byte[sl.size];
                raf.seek(sl.seeker);
                raf.read(buf, 0, sl.size);
                dwg.headerVariables = Dwg.readHeaderVariable(buf, offset, dwg.header.ver);
                break;
            case 1:
                // class section
                buf = new byte[sl.size];
                raf.seek(sl.seeker);
                raf.read(buf, 0, sl.size);
                offset += Dwg.readClassSection(buf, offset, dwg);
                break;
            case 2:
                // object map
                buf = new byte[sl.size];
                raf.seek(sl.seeker);
                raf.read(buf, 0, sl.size);
                offset += Dwg.readObjectMap(buf, offset, dwg, dwg.header.ver);
                break;
            case 3:
                // (C3 and later.) A special table
                break;
            case 4:
                // In r13-R15, points to a location where there may be data stored
                buf = new byte[sl.size];
                raf.seek(sl.seeker);
                raf.read(buf, 0, sl.size);
                offset += Dwg.readData(buf, offset, dwg);
                break;
            default:
                break;
            }
        }
        
        // CLASS DEFINITIONS
        
        // TEMPLATE (R13 Only, optional)
        
        // PADDING (R13c3 AND LATER, 200 bytes, minutes the template section above if present)
        
        // IMAGE DATA (pre-R13c3)
        
        // OBJECT DATA
        //   All entities, table entries, dictionary entries, etc. go in this section
        
        // OBJECT MAP
        
        // OBJECT TREE SPACE (optional)
        
        // TEMPLATE (R14-R15, optional)
        
        // SECOND HEADER
        
        // IMAGE DATA (R13c3 AND LATER)
        
        return offset;
    }
    
    private static short calculateCRC(short seed, int recordsNum) {
        // Not implemented yet.
        // don't know how to calculate
        short ret = 0;
        
        switch(recordsNum) {
        case 3: ret = (short)(seed ^ 0xA598);   break;
        case 4: ret = (short)(seed ^ 0x8101);   break;
        case 5: ret = (short)(seed ^ 0x3CC4);   break;
        case 6: ret = (short)(seed ^ 0x8461);   break;
        }
        
        return ret;
    }
}
