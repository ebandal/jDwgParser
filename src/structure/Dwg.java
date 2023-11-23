package structure;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import decode.DecodeCallback;
import decode.DecoderR14;
import decode.DecoderR18;
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
        //  0xCF,0x7B,0x1F,0x23,0xFD,0xDE,0x38,0xA9,0x5F,0x7C,0x68,0xB8,0x4E,0x6D,0x33,0x5F
        offset.addAndGet(16);
        
        // Size of the section (a 4 byte long)
        int sectionSize = ByteBuffer.wrap(buf, offset.get(), 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        offset.addAndGet(4);
        
        // R2010/R1013 (only present if the maintenance version is greater than 3!) or R2018_:
        //  unknown (4 byte long), might be part of a 62-bit size.
        if (ver.between(DwgVersion.R2010,  DwgVersion.R2013)) {
            
        } else if (ver.from(DwgVersion.R2018)) {
            offset.addAndGet(4);
        }
        
        HeaderVariables hdrVars = new HeaderVariables();
        
        // R2007 Only:
        long sizeInBits = 0;
        if (ver.only(DwgVersion.R2007)) {
            //  RL : Size in bits
            hdrVars.lSizeInBits = readRawLong(buf, offset.get(), bitOffset.get(), "SizeInBits", cb);
        }
        
        // R2013+:
        if (ver.from(DwgVersion.R2013)) {
            // BLL : Variabele REQUIREDVERSIONS, default value 0, read only.
            hdrVars.llRequiredVersions = readBitLongLong(buf, offset.get(), bitOffset.get(), "REQUIREDVERSIONS", cb);
        }
        
        // Common:
        //  BD : Unknown, default value 412148564080.0
        double dUnknown = readBitDouble(buf, offset.get(), bitOffset.get(), "Unknown", cb);
        //  BD : Unknown, default value 1.0
        dUnknown = readBitDouble(buf, offset.get(), bitOffset.get(), "Unknown", cb);
        //  BD : Unknown, default value 1.0
        dUnknown = readBitDouble(buf, offset.get(), bitOffset.get(), "Unknown", cb);
        //  BD : Unknown, default value 1.0
        dUnknown = readBitDouble(buf, offset.get(), bitOffset.get(), "Unknown", cb);
        //  TV : Unknown text string, default ""
        String tUnknown = readVariableText(buf, offset.get(), bitOffset.get(), ver, "Unknown", cb);
        //  TV : Unknown text string, default ""
        tUnknown = readVariableText(buf, offset.get(), bitOffset.get(), ver, "Unknown", cb);
        //  TV : Unknown text string, default ""
        tUnknown = readVariableText(buf, offset.get(), bitOffset.get(), ver, "Unknown", cb);
        //  TV : Unknown text string, default ""
        tUnknown = readVariableText(buf, offset.get(), bitOffset.get(), ver, "Unknown", cb);
        //  BL : Unknown long, default value 24L
        int lUnknown = readBitLong(buf, offset.get(), bitOffset.get(), "Unknown", cb);
        //  BL : Unknown long, default value 0L;
        lUnknown = readBitLong(buf, offset.get(), bitOffset.get(), "Unknown", cb);
        
        // R13-R14 Only:
        if (ver.between(DwgVersion.R13, DwgVersion.R14)) { 
            // BS : Unknown short, default value 0
            short sUnknown = readBitShort(buf, offset.get(), bitOffset.get(), "Unknown", cb);
        }
        
        // Pre-2004 Only:
        if (ver.until(DwgVersion.R2004)) {
            // H : Handle of the current viewport entity header (hard pointer)
            hdrVars.hCurrViewportEntityHeader 
                = readHandle(buf, offset.get(), bitOffset.get(), "Handle of current viewport", cb);
        }
        
        /*
            Common:
            B : DIMASO
            B : DIMSHO
            R13-R14 Only:
            B : DIMSAV Undocumented.
            Common:
            B : PLINEGEN
            B : ORTHOMODE
            B : REGENMODE
            B : FILLMODE
            B : QTEXTMODE
            B : PSLTSCALE
            B : LIMCHECK
            R13-R14 Only (stored in registry from R15 onwards):
            B : BLIPMODE
            R2004+:
            B : Undocumented
            Common:
            B : USRTIMER (User timer on/off).
            B : SKPOLY
            B : ANGDIR
            B : SPLFRAME
            R13-R14 Only (stored in registry from R15 onwards):
            B : ATTREQ
            B : ATTDIA
            Common:
            B : MIRRTEXT
            B : WORLDVIEW
            R13-R14 Only:
            B : WIREFRAME Undocumented.
            Common:
            B : TILEMODE
            B : PLIMCHECK
            B : VISRETAIN
            R13-R14 Only (stored in registry from R15 onwards):
            B : DELOBJ
            Common:
            Open Design Specification for .dwg files 74
            B : DISPSILH
            B : PELLIPSE (not present in DXF)
            BS : PROXYGRAPHICS
            R13-R14 Only (stored in registry from R15 onwards):
            BS : DRAGMODE
            Common:
            BS : TREEDEPTH
            BS : LUNITS
            BS : LUPREC
            BS : AUNITS
            BS : AUPREC
            R13-R14 Only Only (stored in registry from R15 onwards):
            BS : OSMODE
            Common:
            BS : ATTMODE
            R13-R14 Only Only (stored in registry from R15 onwards):
            BS : COORDS
            Common:
            BS : PDMODE
            R13-R14 Only Only (stored in registry from R15 onwards):
            BS : PICKSTYLE
            R2004+:
            BL : Unknown
            BL: Unknown
            BL : Unknown
            Common:
            BS : USERI1
            BS : USERI2
            BS : USERI3
            BS : USERI4
            BS : USERI5
            BS : SPLINESEGS
            BS : SURFU
            BS : SURFV
            BS : SURFTYPE
            BS : SURFTAB1
            BS : SURFTAB2
            BS : SPLINETYPE
            BS : SHADEDGE
            BS : SHADEDIF
            Open Design Specification for .dwg files 75
            BS : UNITMODE
            BS : MAXACTVP
            BS : ISOLINES
            BS : CMLJUST
            BS : TEXTQLTY
            BD : LTSCALE
            BD : TEXTSIZE
            BD : TRACEWID
            BD : SKETCHINC
            BD : FILLETRAD
            BD : THICKNESS
            BD : ANGBASE
            BD : PDSIZE
            BD : PLINEWID
            BD : USERR1
            BD : USERR2
            BD : USERR3
            BD : USERR4
            BD : USERR5
            BD : CHAMFERA
            BD : CHAMFERB
            BD : CHAMFERC
            BD : CHAMFERD
            BD : FACETRES
            BD : CMLSCALE
            BD : CELTSCALE
            R13-R18:
            TV : MENUNAME
            Common:
            BL : TDCREATE (Julian day)
            BL : TDCREATE (Milliseconds into the day)
            BL : TDUPDATE (Julian day)
            BL : TDUPDATE (Milliseconds into the day)
            R2004+:
            BL : Unknown
            BL : Unknown
            BL : Unknown
            Common:
            BL : TDINDWG (Days)
            BL : TDINDWG (Milliseconds into the day)
            Open Design Specification for .dwg files 76
            BL : TDUSRTIMER (Days)
            BL : TDUSRTIMER (Milliseconds into the day)
            CMC : CECOLOR
            H : HANDSEED The next handle, with an 8-bit length specifier preceding the handle
            bytes (standard hex handle form) (code 0). The HANDSEED is not part of the handle
            stream, but of the normal data stream (relevant for R21 and later).
            H : CLAYER (hard pointer)
            H : TEXTSTYLE (hard pointer)
            H : CELTYPE (hard pointer)
            R2007+ Only:
            H : CMATERIAL (hard pointer)
            Common:
            H : DIMSTYLE (hard pointer)
            H : CMLSTYLE (hard pointer)
            R2000+ Only:
            BD : PSVPSCALE
            Common:
            3BD : INSBASE (PSPACE)
            3BD : EXTMIN (PSPACE)
            3BD : EXTMAX (PSPACE)
            2RD : LIMMIN (PSPACE)
            2RD : LIMMAX (PSPACE)
            BD : ELEVATION (PSPACE)
            3BD : UCSORG (PSPACE)
            3BD : UCSXDIR (PSPACE)
            3BD : UCSYDIR (PSPACE)
            H : UCSNAME (PSPACE) (hard pointer)
            R2000+ Only:
            H : PUCSORTHOREF (hard pointer)
            BS : PUCSORTHOVIEW
            H : PUCSBASE (hard pointer)
            3BD : PUCSORGTOP
            3BD : PUCSORGBOTTOM
            3BD : PUCSORGLEFT
            3BD : PUCSORGRIGHT
            3BD : PUCSORGFRONT
            3BD : PUCSORGBACK
            Common:
            3BD : INSBASE (MSPACE)
            3BD : EXTMIN (MSPACE)
            3BD : EXTMAX (MSPACE)
            Open Design Specification for .dwg files 77
            2RD : LIMMIN (MSPACE)
            2RD : LIMMAX (MSPACE)
            BD : ELEVATION (MSPACE)
            3BD : UCSORG (MSPACE)
            3BD : UCSXDIR (MSPACE)
            3BD : UCSYDIR (MSPACE)
            H : UCSNAME (MSPACE) (hard pointer)
            R2000+ Only:
            H : UCSORTHOREF (hard pointer)
            BS : UCSORTHOVIEW
            H : UCSBASE (hard pointer)
            3BD : UCSORGTOP
            3BD : UCSORGBOTTOM
            3BD : UCSORGLEFT
            3BD : UCSORGRIGHT
            3BD : UCSORGFRONT
            3BD : UCSORGBACK
            TV : DIMPOST
            TV : DIMAPOST
            R13-R14 Only:
            B : DIMTOL
            B : DIMLIM
            B : DIMTIH
            B : DIMTOH
            B : DIMSE1
            B : DIMSE2
            B : DIMALT
            B : DIMTOFL
            B : DIMSAH
            B : DIMTIX
            B : DIMSOXD
            RC : DIMALTD
            RC : DIMZIN
            B : DIMSD1
            B : DIMSD2
            RC : DIMTOLJ
            RC : DIMJUST
            RC : DIMFIT
            B : DIMUPT
            RC : DIMTZIN
            Open Design Specification for .dwg files 78
            RC : DIMALTZ
            RC : DIMALTTZ
            RC : DIMTAD
            BS : DIMUNIT
            BS : DIMAUNIT
            BS : DIMDEC
            BS : DIMTDEC
            BS : DIMALTU
            BS : DIMALTTD
            H : DIMTXSTY (hard pointer)
            Common:
            BD : DIMSCALE
            BD : DIMASZ
            BD : DIMEXO
            BD : DIMDLI
            BD : DIMEXE
            BD : DIMRND
            BD : DIMDLE
            BD : DIMTP
            BD : DIMTM
            R2007+ Only:
            BD : DIMFXL
            BD : DIMJOGANG
            BS : DIMTFILL
            CMC : DIMTFILLCLR
            R2000+ Only:
            B : DIMTOL
            B : DIMLIM
            B : DIMTIH
            B : DIMTOH
            B : DIMSE1
            B : DIMSE2
            BS : DIMTAD
            BS : DIMZIN
            BS : DIMAZIN
            R2007+ Only:
            BS : DIMARCSYM
            Common:
            BD : DIMTXT
        )    BD : DIMCEN
            Open Design Specification for .dwg files 79
            BD : DIMTSZ
            BD : DIMALTF
            BD : DIMLFAC
            BD : DIMTVP
            BD : DIMTFAC
            BD : DIMGAP
            R13-R14 Only:
            T : DIMPOST
            T : DIMAPOST
            T : DIMBLK
            T : DIMBLK1
            T : DIMBLK2
            R2000+ Only:
            BD : DIMALTRND
            B : DIMALT
            BS : DIMALTD
            B : DIMTOFL
            B : DIMSAH
            B : DIMTIX
            B : DIMSOXD
            Common:
            CMC : DIMCLRD
            CMC : DIMCLRE
            CMC : DIMCLRT
            R2000+ Only:
            BS : DIMADEC
            BS : DIMDEC
            BS : DIMTDEC
            BS : DIMALTU
            BS : DIMALTTD
            BS : DIMAUNIT
            BS : DIMFRAC
            BS : DIMLUNIT
            BS : DIMDSEP
            BS : DIMTMOVE
            BS : DIMJUST
            B : DIMSD1
            B : DIMSD2
            BS : DIMTOLJ
            BS : DIMTZIN
            Open Design Specification for .dwg files 80
            BS : DIMALTZ
            BS : DIMALTTZ
            B : DIMUPT
            BS : DIMATFIT
            R2007+ Only:
            B : DIMFXLON
            R2010+ Only:
            B : DIMTXTDIRECTION
            BD : DIMALTMZF
            T : DIMALTMZS
            BD : DIMMZF
            T : DIMMZS
            R2000+ Only:
            H : DIMTXSTY (hard pointer)
            H : DIMLDRBLK (hard pointer)
            H : DIMBLK (hard pointer)
            H : DIMBLK1 (hard pointer)
            H : DIMBLK2 (hard pointer)
            R2007+ Only:
            H : DIMLTYPE (hard pointer)
            H : DIMLTEX1 (hard pointer)
            H : DIMLTEX2 (hard pointer)
            R2000+ Only:
            BS : DIMLWD
            BS : DIMLWE
            Common:
            H : BLOCK CONTROL OBJECT (hard owner)
            H : LAYER CONTROL OBJECT (hard owner)
            H : STYLE CONTROL OBJECT (hard owner)
            H : LINETYPE CONTROL OBJECT (hard owner)
            H : VIEW CONTROL OBJECT (hard owner)
            H : UCS CONTROL OBJECT (hard owner)
            H : VPORT CONTROL OBJECT (hard owner)
            H : APPID CONTROL OBJECT (hard owner)
            H : DIMSTYLE CONTROL OBJECT (hard owner)
            R13-R15 Only:
            H : VIEWPORT ENTITY HEADER CONTROL OBJECT (hard owner)
            Common:
            H : DICTIONARY (ACAD_GROUP) (hard pointer)
            H : DICTIONARY (ACAD_MLINESTYLE) (hard pointer)
            Open Design Specification for .dwg files 81
            H : DICTIONARY (NAMED OBJECTS) (hard owner)
            R2000+ Only:
            BS : TSTACKALIGN, default = 1 (not present in DXF)
            BS : TSTACKSIZE, default = 70 (not present in DXF)
            TV : HYPERLINKBASE
            TV : STYLESHEET
            H : DICTIONARY (LAYOUTS) (hard pointer)
            H : DICTIONARY (PLOTSETTINGS) (hard pointer)
            H : DICTIONARY (PLOTSTYLES) (hard pointer)
            R2004+:
            H : DICTIONARY (MATERIALS) (hard pointer)
            H : DICTIONARY (COLORS) (hard pointer)
            R2007+:
            H : DICTIONARY (VISUALSTYLE) (hard pointer)
            R2013+:
            H : UNKNOWN (hard pointer)
            R2000+:
            BL : Flags:
            CELWEIGHT Flags & 0x001F
            ENDCAPS Flags & 0x0060
            JOINSTYLE Flags & 0x0180
            LWDISPLAY !(Flags & 0x0200)
            XEDIT !(Flags & 0x0400)
            EXTNAMES Flags & 0x0800
            PSTYLEMODE Flags & 0x2000
            OLESTARTUP Flags & 0x4000
            BS : INSUNITS
            BS : CEPSNTYPE
            H : CPSNID (present only if CEPSNTYPE == 3) (hard pointer)
            TV : FINGERPRINTGUID
            TV : VERSIONGUID
            R2004+:
            RC : SORTENTS
            RC : INDEXCTL
            RC : HIDETEXT
            RC : XCLIPFRAME, before R2010 the value can be 0 or 1 only.
            RC : DIMASSOC
            RC : HALOGAP
            BS : OBSCUREDCOLOR
            BS : INTERSECTIONCOLOR
            RC : OBSCUREDLTYPE
            Open Design Specification for .dwg files 82
            RC : INTERSECTIONDISPLAY
            TV : PROJECTNAME
            Common:
            H : BLOCK_RECORD (*PAPER_SPACE) (hard pointer)
            H : BLOCK_RECORD (*MODEL_SPACE) (hard pointer)
            H : LTYPE (BYLAYER) (hard pointer)
            H : LTYPE (BYBLOCK) (hard pointer)
            H : LTYPE (CONTINUOUS) (hard pointer)
            R2007+:
            B : CAMERADISPLAY
            BL : unknown
            BL : unknown
            BD : unknown
            BD : STEPSPERSEC
            BD : STEPSIZE
            BD : 3DDWFPREC
            BD : LENSLENGTH
            BD : CAMERAHEIGHT
            RC : SOLIDHIST
            RC : SHOWHIST
            BD : PSOLWIDTH
            BD : PSOLHEIGHT
            BD : LOFTANG1
            BD : LOFTANG2
            BD : LOFTMAG1
            BD : LOFTMAG2
            BS : LOFTPARAM
            RC : LOFTNORMALS
            BD : LATITUDE
            BD : LONGITUDE
            BD : NORTHDIRECTION
            BL : TIMEZONE
            RC : LIGHTGLYPHDISPLAY
            RC : TILEMODELIGHTSYNCH
            RC : DWFFRAME
            RC : DGNFRAME
            B : unknown
            CMC : INTERFERECOLOR
            H : INTERFEREOBJVS (hard pointer)
            H : INTERFEREVPVS (hard pointer)
            Open Design Specification for .dwg files 83
            H : DRAGVS (hard pointer)
            RC : CSHADOW
            BD : unknown
            R14+:
            BS : unknown short (type 5/6 only) these do not seem to be required,
            BS : unknown short (type 5/6 only) even for type 5.
            BS : unknown short (type 5/6 only)
            BS : unknown short (type 5/6 only)
            Common:
            RS : CRC for the data section, starting after the sentinel. Use 0xC0C1 for the initial
            value.
        */
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
    
    public static short readBitShort(byte[] buf, int off, int bitOff, String name, DecodeCallback cb) {
        int retBitOffset = 0;
        int offset = off;
        int bitOffset = bitOff;
        byte[] dBuf = new byte[2];
        short value = 0;
        
        byte bitControl = bitOffset==7 ? (byte)((buf[offset]<<1 & 0x02)|(buf[offset+1]>>7 & 0x01)) : (byte)(buf[offset]>>(8-bitOffset-2) & 0x03);
        bitOffset += 2; retBitOffset += 2;
        offset += bitOffset/8;
        bitOffset = bitOffset%8;
        
        switch(bitControl) {
        case 0: // A short (2 bytes) follows, little-endian order (LSB first)
            for (int i=0; i<2; i++) {
                dBuf[i] = (byte) ((((buf[offset+i+1]&0x00FF)>>(8-bitOffset))&0xFF) | (buf[offset+i]<<bitOffset&0xFF));
            }
            value = ByteBuffer.wrap(dBuf, 0, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            offset += 2; retBitOffset += 16;
            break;
        case 1: // An unsgined char (1 byte) follow
            value = (short) ((((buf[offset+1]&0x00FF)>>(8-bitOffset))&0xFF) | (buf[offset]<<bitOffset&0xFF));
            offset += 1; retBitOffset += 8;
            break;
        case 2: // 0
            value = 0;
            break;
        case 3: // 256
            value = 256;
            break;
        }
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
        long value = 0;
        byte[] dBuf = new byte[8];
        
        byte length = bitOffset==7 ? (byte)((buf[offset]<<1 & 0x02)|(buf[offset+1]>>7 & 0x01)) : (byte)(buf[offset]>>(8-bitOffset-2) & 0x03);
        bitOffset += 2; retBitOffset += 2;
        offset += bitOffset/8;
        bitOffset = bitOffset%8;
        
        for (int i=0; i<length; i++) {
            dBuf[i] = (byte) ((((buf[offset+i+1]&0x00FF)>>(8-bitOffset))&0xFF) | (buf[offset+i]<<bitOffset&0xFF));
        }
        value = ByteBuffer.wrap(dBuf, 0, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        offset += 8; retBitOffset += 64;
        cb.onDecoded(name, value, retBitOffset);
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
    
    private static HandleRef readHandle(byte[] buf, int off, int bitOff, String name, DecodeCallback cb) {
        HandleRef handleRef = new HandleRef();

        int retBitOffset = 0;
        int offset = off;
        byte firstByte = (byte) ((((buf[offset+1]&0x00FF)>>(8-bitOff))&0xFF) | (buf[offset]<<bitOff&0xFF));
        handleRef.code = (byte) (firstByte>>4&0x0F);
        handleRef.counter = (byte) (firstByte&0x0F);
        handleRef.handle = new byte[handleRef.counter];
        offset += 1; retBitOffset += 8;

        for (int i=0; i<handleRef.counter; i++) {
            handleRef.handle[i] = (byte) ((((buf[offset+i+1]&0x00FF)>>(8-bitOff))&0xFF) | (buf[offset+i]<<bitOff&0xFF));
        }
        offset += handleRef.counter; 
        retBitOffset += handleRef.counter*8;
        cb.onDecoded(name, handleRef, retBitOffset);
        
        return handleRef;
    }
    
    private static CmColor readCmColor(byte[] buf, int off, int bitOff, DwgVersion ver, String name, DecodeCallback cb) {
        int retBitOffset = 0;
        int offset = off;
        int bitOffset = bitOff;
        byte[] dBuf = null;
        
        CmColor cmColor = new CmColor();
        
        byte bitControl = bitOffset==7 ? (byte)((buf[offset]<<1 & 0x02)|(buf[offset+1]>>7 & 0x01)) : (byte)(buf[offset]>>(8-bitOffset-2) & 0x03);
        bitOffset += 2; retBitOffset += 2;
        offset += bitOffset/8;
        bitOffset = bitOffset%8;
        dBuf = new byte[2];
        switch(bitControl) {
        case 0: // A short(2bytes) follows, little-endian order (LSB first)
            for (int i=0; i<2; i++) {
                dBuf[i] = (byte)((((buf[offset+i+1]&0x00FF)>>(8-bitOffset))&0xFF) | (buf[offset+i]<<bitOffset&0xFF));
            }
            cmColor.colorIndex = ByteBuffer.wrap(dBuf, 0, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            offset += 2; retBitOffset += 16;
            break;
        case 1: // An unsigned char (1 byte) follows
            cmColor.colorIndex = (short) ((((buf[offset+1]&0x00FF)>>(8-bitOffset))&0xFF) | (buf[offset]<<bitOffset&0xFF));
            offset += 1; retBitOffset += 8;
            break;
        case 2: // 0
            cmColor.colorIndex = 0;
            break;
        case 3: // 256
            cmColor.colorIndex = 256;
            break;
        }
        
        if (ver.from(DwgVersion.R2004)) {
            bitControl = bitOffset==7 ? (byte)((buf[offset]<<1 & 0x02) | (buf[offset+1]>>7 & 0x01)) : (byte)(buf[offset]>>(8-bitOffset-2) & 0x03);
            bitOffset += 2; retBitOffset += 2;
            offset += bitOffset/8;
            bitOffset = bitOffset%8;
            dBuf = new byte[4];
            switch(bitControl) {
            case 0: // A long(4bytes) follows, little-endian order (LSB first)
                for (int i=0; i<4; i++ ) {
                    dBuf[i] = (byte) ((((buf[offset+i+1]&0x00FF)>>(8-bitOffset))&0xFF) | (buf[offset+i]<<bitOffset&0xFF));
                }
                cmColor.rgbValue = ByteBuffer.wrap(dBuf, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
                offset += 4; retBitOffset += 32;
                break;
            case 1: // An unsigned char (1 byte) follows
                cmColor.rgbValue = ((((buf[offset+1]&0x00FF)>>(8-bitOffset))&0xFF) | (buf[offset]<<bitOffset&0xFF));
                offset += 1; retBitOffset += 8;
                break;
            case 2: // 0
                cmColor.rgbValue = 0;
                break;
            case 3: // not used
                break;
            }
            
            cmColor.colorByte = (byte) ((((buf[offset+1]&0x00FF)>>(8-bitOffset))&0xFF) | (buf[offset]<<bitOffset&0xFF));
            offset += 1; retBitOffset += 8;
        }
        
        cb.onDecoded(name, cmColor, retBitOffset);
        return cmColor;
    }
    
    
    private static long readModularChar(byte[] buf, int off, String name, DecodeCallback cb) {
        int offset = off;
        byte[] step1Bytes = new byte[8];
        int idx = 0;
        
        while(true) {
            step1Bytes[idx] = (byte)(buf[offset+idx]);
            if ((step1Bytes[idx]&0x80) == 0x00) {
                break;
            }
            idx += 1;
        }
        idx += 1;
        
        byte[] step2Bytes = new byte[idx];
        for (int i=0; i<idx; i++) {
            step2Bytes[i] = (byte)((byte)((step1Bytes[idx-1-i]&0x007F)>>(idx-1-i)) | (i>0 ? ((step1Bytes[idx-i]&0x7F)<<(8-(idx-i))) : 0x00));
        }
        
        long sum = 0;
        for (int i=0; i<idx; i++) {
            sum += ((step2Bytes[i]&0x00FF) * (long)Math.pow(256, (idx-1-i)));
        }
        cb.onDecoded(name,  sum, (idx+1)*8);
        
        return sum;
    }
    
    public static int readClassSection(byte[] buf, int offset, Dwg dwg) {
        // TODO Auto-generated method stub
        return 0;
    }

    public static int readObjectMap(byte[] buf, int off, Dwg dwg, DwgVersion ver) {
        AtomicInteger offset = new AtomicInteger(off);
        AtomicInteger bitOffset = new AtomicInteger(0);
        int retBitOffset = 0;
        
        Map<Long, Long> objectMap = new HashMap<>();
        
        // page 251
        if (ver.between(DwgVersion.R13, DwgVersion.R15)) {
            // 
            DecodeCallback cb = new DecodeCallback() {
                public void onDecoded(String name, Object value, int retBitOffset) {
                    // log.info("[" + name + "] = (" + value.toString() + ")");
                    offset.addAndGet((bitOffset.get()+retBitOffset)/8);
                    bitOffset.set((bitOffset.get()+retBitOffset)%8);
                }
            };
            
            short sectionSize = ByteBuffer.wrap(buf, offset.get(), 2).order(ByteOrder.BIG_ENDIAN).getShort();
            offset.addAndGet(2);
            
            while(offset.get() < sectionSize) {
                long handleOffset = readModularChar(buf, offset.get(), "Handle Offset", cb);
                long locationOffset = readModularChar(buf, offset.get(), "Location Offset", cb);
                
                objectMap.put(handleOffset,  locationOffset);
            }
            short crc = ByteBuffer.wrap(buf, offset.get(), 2).order(ByteOrder.BIG_ENDIAN).getShort();
            offset.addAndGet(2);
        } else if (ver.from(DwgVersion.R18)) {
            byte[] decomBuf = DecoderR18.decompressR18(buf, offset.get());
            AtomicInteger decomOffset = new AtomicInteger(0);
            
            decomOffset.addAndGet(32);
            
            DecodeCallback cb = new DecodeCallback() {
                public void onDecoded(String name, Object value, int regBitOffset) {
                    offset.addAndGet((bitOffset.get()+retBitOffset)/8);
                    bitOffset.set((bitOffset.get()+retBitOffset)%8);
                }
            };
            
            short sectionSize = ByteBuffer.wrap(decomBuf, decomOffset.get(), 2).order(ByteOrder.BIG_ENDIAN).getShort();
            decomOffset.addAndGet(2);
            
            while(offset.get() < sectionSize) {
                long handleOffset = readModularChar(decomBuf, decomOffset.get(), "Handle Offset", cb);
                long locationOffset = readModularChar(decomBuf, decomOffset.get(), "Location Offset", cb);
                
                objectMap.put(handleOffset,  locationOffset);
            }
            short crc = ByteBuffer.wrap(decomBuf, decomOffset.get(), 2).order(ByteOrder.BIG_ENDIAN).getShort();
            decomOffset.addAndGet(2);
        }
        
        
        // TODO Auto-generated method stub
        return 0;
    }




}
