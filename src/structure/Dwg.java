package structure;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.logging.Logger;

import decode.DecodeCallback;
import decode.DecoderR14;
import decode.DwgParseException;
import structure.header.FileHeader;
import structure.sectionpage.DataSectionPage;
import structure.sectionpage.HeaderVariables;
import structure.sectionpage.SystemSectionPage;

public class Dwg {
    private static final Logger log = Logger.getLogger(Dwg.class.getName());
    
    public FileHeader header;
    public HeaderVariables headerVariables;
    
    public List<SystemSectionPage> systemSectionPageList;
    public List<DataSectionPage> dataSectionPageList;
    
    public void decode(File dwgFile) throws DwgParseException {
        
        try (RandomAccessFile raf = new RandomAccessFile(dwgFile, "r")) {
        
            // read header
            header = readFileHeader(raf);
            
            // read body
            readFileBody(raf);
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void readFileBody(RandomAccessFile raf) throws IOException, DwgParseException {
        int offset = 0;
        
        switch(header.versionId) {
        case "AC1012":
            header.ver = DwgVersion.R13;
            throw new DwgParseException();
        case "AC1014":
            header.ver = DwgVersion.R14;
            offset += DecoderR14.readData(raf, this);
            break;
        case "AC1015":
            header.ver = DwgVersion.R2000;
            offset += DecoderR14.readData(raf, this);
            break;
        case "AC1018":
            header.ver = DwgVersion.R2004;
            break;
        case "AC1021":
            header.ver = DwgVersion.R2007;
            break;
        case "AC1024":
            header.ver = DwgVersion.R2010;
            break;
        case "AC1027":
            header.ver = DwgVersion.R2013;
            break;
        case "AC1032":
            header.ver = DwgVersion.R2018;
            break;
        }
    }
    
    public FileHeader readFileHeader(RandomAccessFile raf) throws IOException, DwgParseException {
        FileHeader fileHeader = new FileHeader();
        
        byte[] buf = null;
        int offset = 0;
        
        // VERSION ID
        byte[] versionIdBuf = new byte[6];
        int readLen = raf.read(versionIdBuf, offset, 6);
        fileHeader.versionId = new String(versionIdBuf, 0, 6, StandardCharsets.US_ASCII);
        log.finest("VersionId: " + fileHeader.versionId);

        DwgVersion ver;
        switch(fileHeader.versionId) {
        case "AC1012":
            fileHeader.ver = DwgVersion.R13;
            throw new DwgParseException();
        case "AC1014":
            fileHeader.ver = DwgVersion.R14;
            offset = 6;
            offset += DecoderR14.readFileHeader(raf, offset, fileHeader);
            break;
        case "AC1015":
            fileHeader.ver = DwgVersion.R2000;
            offset += DecoderR14.readFileHeader(raf,  offset, fileHeader);
            break;
        case "AC1018":
            fileHeader.ver = DwgVersion.R2004;
            buf = new byte[0x100];
            offset += 6;
            readLen = raf.read(buf, offset, 0x100-6);
            // offset += DecoderR2004.readFileHeader(buf, offset, fileHeader);
            break;
        case "AC1021":
            fileHeader.ver = DwgVersion.R2007;
            break;
        case "AC1024":
            fileHeader.ver = DwgVersion.R2010;
            break;
        case "AC1027":
            fileHeader.ver = DwgVersion.R2013;
            break;
        case "AC1032":
            fileHeader.ver = DwgVersion.R2018;
            break;
        }
        
        // six bytes of 0 (in R14, 5 0's and the ACADMAINTVER)
        offset += 6;
            
        
        return fileHeader;
    }

    public static int readClassSection(byte[] buf, int offset, Dwg dwg) {
        // TODO Auto-generated method stub
        return 0;
    }

    public static int readObjectMap(byte[] buf, int offset, Dwg dwg) {
        // TODO Auto-generated method stub
        return 0;
    }

    public static HeaderVariables readHeaderVariable(byte[] buf, int off, DwgVersion ver) {
        AtomicInteger offset = new AtomicInteger(off);
        AtomicInteger bitOffset = new AtomicInteger(0);
        int retBitOffset = 0;
        
        DecodeCallback cb = new DecodeCallback() {
            public void onDecoded(String name, Object value, int retBitOffset) {
                log.info("[" + name + "] = (" + value.toString() + ")");
                offset.addAndGet((bitOffset.get()+retBitOffset)/8);
                bitOffset.set((bitOffset.get()+retBitOffset)%8);
            }
        };
        
        // beginning sentinel
        // 
        offset.addAndGet(16);
        
        // Size of the section (a 4 byte long)
        int size = ByteBuffer.wrap(buf, offset.get(), 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        offset.addAndGet(4);
        
        // R2010/R1013 (only present if the maintenance version is greater than 3!) or R2018_:
        //  unknown (4 byte long), might be part of a 62-bit size.
        if (ver.between(DwgVersion.R2010,  DwgVersion.R2013)) {
            
        } else if (ver.from(DwgVersion.R2018)) {
            offset.addAndGet(4);
        }
        
        HeaderVariables hdrVars = new HeaderVariables();
        // DATA
        // R2007 Only:
        //  RL : Size in bits
        long sizeInBits = 0;
        if (ver.only(DwgVersion.R2007)) {
            hdrVars.lSizeInBits = readRawLong(buf, offset.get(), bitOffset.get(), "SizeInBits", cb);
        }
        
        return hdrVars;
    }
    
    private static double readBitDouble(byte[] buf, int off, int bitOff, String name, DecodeCallback cb) {
        int retBitOffset = 0;
        int offset = off;
        int bitOffset = bitOff;
        byte[] dBuf = new byte[8];
        double value = 0.0;
        
        byte bitControl = bitOffset==7 ? (byte)((buf[offset]<<1 & 0x02)|(buf[offset+1]>>7 & 0x01)) : (byte)(buf[offset]>>(8-bitOffset-2) & 0x03);
        bitOffset += 2; retBitOffset += 2;
        offset += bitOffset/8;
        bitOffset = bitOffset%8;
        
        switch(bitControl) {
        case 0: // A double follows
            for (int i=0; i<8; i++) {
                dBuf[i] = (byte) ((((buf[offset+i+1]&0x00FF)>>(8-bitOffset))&0xFF) | (buf[offset+i]<<bitOffset&0xFF));
            }
            long lValue = ByteBuffer.wrap(buf, 0, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
            value = Double.longBitsToDouble(lValue);
            offset += 8; retBitOffset += 64;
            cb.onDecoded(name, value, retBitOffset);
            break;
        case 1: // 1.0
            value = 1.0;
            cb.onDecoded(name, value, retBitOffset);
            break;
        case 2: // 0.0
            value = 0.0;
            cb.onDecoded(name, value, retBitOffset);
            break;
        case 3: // not used
            break;
        }
        
        return value;
    }
    
    private static String readVariableText(byte[] buf, int off, int bitOff, DwgVersion ver, String name, DecodeCallback cb) {
        if (ver.until(DwgVersion.R2004)) {
            return readText(buf, off, bitOff, name, cb);
        } else {
            return readUnicodeText(buf, off, bitOff, name, cb);
        }
    }
    
    private static String readText(byte[] buf, int off, int bitOff, String name, DecodeCallback cb) {
        int retBitOffset = 0;
        int offset = off;
        int bitOffset = bitOff;
        byte[] dBuf = new byte[2];
        short textLength = 0;
        
        byte bitControl = bitOffset==7 ? (byte)((buf[offset]<<1 & 0x02)|(buf[offset+1]>>7 & 0x01)) : (byte)(buf[offset]>>(8-bitOffset-2) & 0x03);
        bitOffset += 2; retBitOffset += 2;
        offset += bitOffset/8;
        bitOffset = bitOffset%8;
        
        // text (bitshort length, followed by the string)
        switch(bitControl) {
        case 0: // A short (2 bytes) follows, little-endian order (LSB first)
            for (int i=0; i<2; i++) {
                dBuf[i] = (byte) ((((buf[offset+i+1]&0x00FF)>>(8-bitOffset))&0xFF) | (buf[offset+i]<<bitOffset&0xFF));
            }
            offset += 2; retBitOffset += 16;
            textLength = ByteBuffer.wrap(dBuf, 0, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            break;
        case 1: // An unsigned char (1 byte) follows
            textLength = (short) ((((buf[offset+1]&0x00FF)>>(8-bitOffset))&0xFF) | (buf[offset]<<bitOffset&0xFF));
            offset += 1; retBitOffset += 8;
            break;
        case 2: // 0
            textLength = 0;
            break;
        case 3: // 256
            textLength = 256;
            break;
        }
        
        dBuf = new byte[textLength];
        for (int i=0; i<textLength; i++) {
            dBuf[i] = (byte)((((buf[offset+i+1]&0x00FF)>>(8-bitOffset))&0xFF) | (buf[offset+i]<<bitOffset&0xFF));
        }
        offset += textLength; retBitOffset += (textLength*8);
        String value = new String(dBuf, 0, textLength, StandardCharsets.US_ASCII);
        cb.onDecoded(name, value, retBitOffset);
        
        return value;
    }
    
    private static String readUnicodeText(byte[] buf, int off, int bitOff, String name, DecodeCallback cb) {
        int retBitOffset = 0;
        int offset = off;
        int bitOffset = bitOff;
        byte[] dBuf = new byte[2];
        short textLength = 0;
        
        byte bitControl = bitOffset==7 ? (byte)((buf[offset]<<1 & 0x02)|(buf[offset+1]>>7 & 0x01)) : (byte)(buf[offset]>>(8-bitOffset-2) & 0x03);
        bitOffset += 2; retBitOffset += 2;
        offset += bitOffset/8;
        bitOffset = bitOffset%8;
        
        // text (bitshort length, followed by the string)
        switch(bitControl) {
        case 0: // A short (2 bytes) follows, little-endian order (LSB first)
            for (int i=0; i<2; i++) {
                dBuf[i] = (byte) ((((buf[offset+i+1]&0x00FF)>>(8-bitOffset))&0xFF) | (buf[offset+i]<<bitOffset&0xFF));
            }
            offset += 2; retBitOffset += 16;
            textLength = ByteBuffer.wrap(dBuf, 0, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            break;
        case 1: // An unsigned char (1 byte) follows
            textLength = (short) ((((buf[offset+1]&0x00FF)>>(8-bitOffset))&0xFF) | (buf[offset]<<bitOffset&0xFF));
            offset += 1; retBitOffset += 8;
            break;
        case 2: // 0
            textLength = 0;
            break;
        case 3: // 256
            textLength = 256;
            break;
        }
        
        dBuf = new byte[textLength];
        for (int i=0; i<textLength; i++) {
            dBuf[i] = (byte)((((buf[offset+i+1]&0x00FF)>>(8-bitOffset))&0xFF) | (buf[offset+i]<<bitOffset&0xFF));
        }
        offset += textLength; retBitOffset += (textLength*8);
        String value = new String(dBuf, 0, textLength, StandardCharsets.UTF_16LE);
        cb.onDecoded(name, value, retBitOffset);
        
        return value;
    }
    
    public static int readBitLong(byte[] buf, int off, int bitOff, String name, DecodeCallback cb) {
        int retBitOffset = 0;
        int offset = off;
        int bitOffset = bitOff;
        byte[] dBuf = new byte[4];
        int value = 0;
        
        byte bitControl = bitOffset==7 ? (byte)((buf[offset]<<1 & 0x02)|(buf[offset+1]>>7 & 0x01)) : (byte)(buf[offset]>>(8-bitOffset-2) & 0x03);
        bitOffset += 2; retBitOffset += 2;
        offset += bitOffset/8;
        bitOffset = bitOffset%8;
        
        switch(bitControl) {
        case 0: // A long (4bytes) follows, little-endian order (LSB first)
            for (int i=0; i<4; i++) {
                dBuf[i] = (byte) ((((buf[offset+i+1]&0x00FF)>>(8-bitOffset))&0xFF) | (buf[offset+i]<<bitOffset&0xFF));
            }
            value = ByteBuffer.wrap(dBuf, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            offset += 4; retBitOffset += 32;
            cb.onDecoded(name, value, retBitOffset);
            break;
        case 1: // An unsgined char (1 byte) follow
            value = ((((buf[offset+1]&0x00FF)>>(8-bitOffset))&0xFF) | (buf[offset]<<bitOffset&0xFF));
            offset += 1; retBitOffset += 8;
            cb.onDecoded(name, value, retBitOffset);
            break;
        case 2: // 0
            value = 0;
            cb.onDecoded(name, value, retBitOffset);
            break;
        case 3: // not used
            break;
        }
        return value;
    }

    private static long readBitLongLong(byte[] buf, int off, int bitOff, String name, DecodeCallback cb) {
        int retBitOffset = 0;
        int offset = off;
        int bitOffset = bitOff;
        byte[] dBuf = new byte[8];
        long value = 0;
        
        byte bitControl = bitOffset==7 ? (byte)((buf[offset]<<1 & 0x02)|(buf[offset+1]>>7 & 0x01)) : (byte)(buf[offset]>>(8-bitOffset-2) & 0x03);
        bitOffset += 2; retBitOffset += 2;
        offset += bitOffset/8;
        bitOffset = bitOffset%8;
        
        switch(bitControl) {
        case 0: // A long (4bytes) follows, little-endian order (LSB first)
            for (int i=0; i<8; i++) {
                dBuf[i] = (byte) ((((buf[offset+i+1]&0x00FF)>>(8-bitOffset))&0xFF) | (buf[offset+i]<<bitOffset&0xFF));
            }
            value = ByteBuffer.wrap(dBuf, 0, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
            offset += 8; retBitOffset += 64;
            cb.onDecoded(name, value, retBitOffset);
            break;
        /*
        case 1: // An unsgined char (1 byte) follow
            value = ((((buf[offset+1]&0x00FF)>>(8-bitOffset))&0xFF) | (buf[offset]<<bitOffset&0xFF));
            offset += 1; retBitOffset += 8;
            cb.onDecoded(name, value, retBitOffset);
            break;
        case 2: // 0
            value = 0;
            cb.onDecoded(name, value, retBitOffset);
            break;
        case 3: // not used
            break;
        */
        }
        return value;
    }
    
    private static int readRawLong(byte[] buf, int i, int j, String string, DecodeCallback cb) {
        // TODO Auto-generated method stub
        return 0;
    }

    public static int readData(byte[] buf, int offset, Dwg dwg) {
        // TODO Auto-generated method stub
        return 0;
    }
}
