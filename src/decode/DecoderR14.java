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
        int maxSeeker = 0;
        
        for (SectionLocator sl: dwg.header.sectionLocatorList) {
            switch(sl.number) {
            case 0:
                // header variable
                buf = new byte[sl.size];
                raf.seek(sl.seeker);
                raf.read(buf, 0, sl.size);
                dwg.headerVariables = Dwg.readHeaderVariable(buf, offset, dwg.header.ver);
                maxSeeker = Math.max(maxSeeker, sl.seeker+sl.size);
                offset += sl.size;
                break;
            case 1:
                // class section
                buf = new byte[sl.size];
                raf.seek(sl.seeker);
                raf.read(buf, 0, sl.size);
                dwg.drawingClassMap = Dwg.readClassSection(buf, offset, dwg);
                maxSeeker = Math.max(maxSeeker, sl.seeker+sl.size);
                offset += sl.size;
                break;
            case 2:
                // object map
                buf = new byte[sl.size];
                raf.seek(sl.seeker);
                raf.read(buf, 0, sl.size);
                Dwg.read_AcDb_Handles(buf, 0, dwg, dwg.header.ver);
                maxSeeker = Math.max(maxSeeker, sl.seeker+sl.size);
                break;
            case 3:
                // (C3 and later.) A special table
                buf = new byte[sl.size];
                raf.seek(sl.seeker);
                raf.read(buf, 0, sl.size);
                Dwg.read_UnknownSection(buf, 0, dwg, dwg.header.ver);
                maxSeeker = Math.max(maxSeeker, sl.seeker+sl.size);
                break;
            case 4:
                // In r13-R15, points to a location where there may be data stored
                buf = new byte[sl.size];
                raf.seek(sl.seeker);
                raf.read(buf, 0, sl.size);
                Dwg.read_AcDb_Template(buf, 0, dwg);
                maxSeeker = Math.max(maxSeeker, sl.seeker+sl.size);
                break;
            case 5:
                buf = new byte[sl.size];
                raf.seek(sl.seeker);
                raf.read(buf, 0, sl.size);
                maxSeeker = Math.max(maxSeeker, sl.seeker+sl.size);
                break;
            default:
                break;
            }
        }
        
        // SECOND HEADER
        buf = new byte[dwg.header.imageSeeker-maxSeeker];
        raf.seek(maxSeeker);
        raf.read(buf, 0, dwg.header.imageSeeker-maxSeeker);
        Dwg.read_SecondFileHeader(buf, 0, dwg);
        
        // IMAGE DATA (R13c3 AND LATER)
        if (dwg.header.ver.from(DwgVersion.R14)) {
            int imageSize = (int) (raf.length()-raf.getFilePointer());
            buf = new byte[imageSize];
            raf.read(buf, 0, imageSize);
            Dwg.read_AcDb_Preview(buf, 0, dwg);
        }
        
        return offset;
    }
    
}
