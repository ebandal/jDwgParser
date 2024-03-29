package structure;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.JulianFields;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import decode.DecodeCallback;
import decode.DecoderR14;
import decode.DecoderR2004;
import decode.DwgCrcMismatchException;
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
    public Map<Integer, DrawingClass> drawingClassMap;
    
    public Map<Long, Long> data_AcDb_Handles;       // R13-R15, R18
    public String data_AcDb_Tempate;                // R13-R15, R18+

    public void checkVersion(File dwgFile) {
        try (RandomAccessFile raf = new RandomAccessFile(dwgFile, "r")) {
            // read header
            header = readFileHeader(raf);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DwgParseException e) {
            e.printStackTrace();
        } catch (DwgCrcMismatchException e) {
            e.printStackTrace();
        }
    }

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
            offset += DecoderR2004.readSectionPage(raf, this);
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
            buf = new byte[0x480];
            offset = 6;
            readLen = raf.read(buf, offset, 0x480-6);
            offset += DecoderR2007.readMetaData(buf, offset, fileHeader);
            offset += DecoderR2007.readFileHeader(buf, offset, fileHeader);
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
        
        return fileHeader;
    }

    public FileHeader readVersionId(RandomAccessFile raf) throws IOException, DwgParseException {
        FileHeader fileHeader = new FileHeader();

        byte[] buf = new byte[2048];
        int offset = 0;

        // VERSION ID
        int readLen = raf.read(buf, offset, 6);
        fileHeader.versionId = new String(buf, offset, readLen, StandardCharsets.US_ASCII);
        log.finest("VersionId: " + fieHeader.versionId);

        DwgVersion ver;
        switch(fileHeader.versionId) {
        case "AC1012": fileHeader.ver = DwgVersion.R13;     break;
        case "AC1014": fileHeader.ver = DwgVersion.R14;     break;
        case "AC1015": fileHeader.ver = DwgVersion.R2000;   break;
        case "AC1018": fileHeader.ver = DwgVersion.R2004;   break;
        case "AC1021": fileHeader.ver = DwgVersion.R2007;   break;
        case "AC1024": fileHeader.ver = DwgVersion.R2010;   break;
        case "AC1027": fileHeader.ver = DwgVersion.R2013;   break;
        case "AC1032": fileHeader.ver = DwgVersion.R2018;   break;
        }
        // next 7 starting at offset 0x06 are bytes of 0
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
        
        // R2010/R1013 (only present if the maintenance version is greater than 3!) or R2018+:
        //  unknown (4 byte long), might be part of a 62-bit size.
        if (ver.between(DwgVersion.R2010,  DwgVersion.R2013)) {
            
        } else if (ver.from(DwgVersion.R2018)) {
            offset.addAndGet(4);
        }
        
        HeaderVariables hdrVars = new HeaderVariables();
        
        // R2007 Only:
        //  RL : Size in bits
        long sizeInBits = 0;
        if (ver.only(DwgVersion.R2007)) {
            hdrVars.lSizeInBits = readRawLong(buf, offset.get(), bitOffset.get(), "SizeInBits", cb);
        }
        
        // R2013+:
        // BLL : Variabele REQUIREDVERSIONS, default value 0, read only.
        if (ver.from(DwgVersion.R2013)) {
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
        tUnknown = readVariableText(buf, offset.get(), bitOffset.get(), ver, "Unknown", cb);
        //  TV : Unknown text string, default ""
        tUnknown = readVariableText(buf, offset.get(), bitOffset.get(), ver, "Unknown", cb);
        //  TV : Unknown text string, default ""
        tUnknown = readVariableText(buf, offset.get(), bitOffset.get(), ver, "Unknown", cb);
        //  TV : Unknown text string, default ""
        tUnknown = readVariableText(buf, offset.get(), bitOffset.get(), ver, "Unknown", cb);
        int lUnknown;
        //  BL : Unknown long, default value 24L
        lUnknown = readBitLong(buf, offset.get(), bitOffset.get(), "Unknown", cb);
        //  BL : Unknown long, default value 0L;
        lUnknown = readBitLong(buf, offset.get(), bitOffset.get(), "Unknown", cb);

        short sUnknown;
        // R13-R14 Only:
        // BS : Unknown short, default value 0
        if (ver.between(DwgVersion.R13, DwgVersion.R14)) { 
            sUnknown = readBitShort(buf, offset.get(), bitOffset.get(), "Unknown", cb);
        }
        
        // Pre-2004 Only:
        // H : Handle of the current viewport entity header (hard pointer)
        if (ver.until(DwgVersion.R2004)) {
            hdrVars.hCurrViewportEntityHeader = readHandle(buf, offset.get(), bitOffset.get(), "Handle of current viewport", cb);
        }
        
        // B : DIMASO
        hdrVars.bDimaso = readBit(buf, offset.get(), bitOffset.get(), "DIMASO", cb);
        // B : DIMSHO
        hdrVars.bDimsho = readBit(buf, offset.get(), bitOffset.get(), "DIMSHO", cb);

        if (ver.between(DwgVersion.R13, DwgVersion.R14)) {
            hdrVars.bDimsav = readBit(buf, offset.get(), bitOffset.get(), "DIMSAV", cb);
        }

        // B : PLINEGEN
        hdrVars.bPlinegen = readBit(buf, offset.get(), bitOffset.get(), "PLINEGEN", cb);
        // B : ORTHOMODE
        hdrVars.bOrthomode = readBit(buf, offset.get(), bitOffset.get(), "ORTHOMODE", cb);
        // B : REGENMODE
        hdrVars.bRegenmode = readBit(buf, offset.get(), bitOffset.get(), "REGENMODE", cb);
        // B : FILLMODE
        hdrVars.bFillmode = readBit(buf, offset.get(), bitOffset.get(), "FILLMODE", cb);
        // B : QTEXTMODE
        hdrVars.bQtextmode = readBit(buf, offset.get(), bitOffset.get(), "QTEXTMODE", cb);
        // B : PSLTSCALE
        hdrVars.bPsltscale = readBit(buf, offset.get(), bitOffset.get(), "PSLTSCALE", cb);
        // B : LIMCHECK
        hdrVars.bLimcheck = readBit(buf, offset.get(), bitOffset.get(), "LIMCHECK", cb);

        // R13-R14 Only (stored in registry from R15 onwards)
        // B : BLIPMODE
        if (ver.between(DwgVersion.R13, DwgVersion.R14)) {
            hdrVars.bBlipmode = readBit(buf, offset.get(), bitOffset.get(), "BLIPMODE", cb);
        }

        // R2004+
        // B : Undocumented
        if (ver.from(DwgVersion.R2004)) {
            hdrVars.bUndocumented = readBit(buf, offset.get(), bitOffset.get(), "Undocumented", cb);
        }

        // Common
        // B : USRTIMER (User timer on/off).
        hdrVars.bUsrtimer = readBit(buf, offset.get(), bitOffset.get(), "USRTIMER", cb);
        // B : SKPOLY
        hdrVars.bSkpoly = readBit(buf, offset.get(), bitOffset.get(), "SKPOLY", cb);
        // B : ANGDIR
        hdrVars.bAngdir = readBit(buf, offset.get(), bitOffset.get(), "ANGDIR", cb);
        // B : SPLFRAME
        hdrVars.bSplframe = readBit(buf, offset.get(), bitOffset.get(), "SPLFRAME", cb);

        // R13-R14 Only (stored in registry from R15 onwards)
        if (ver.between(DwgVersion.R13, DwgVersion.R14)) {
            // B : ATTREQ
            hdrVars.bAttreq = readBit(buf, offset.get(), bitOffset.get(), "ATTREQ", cb);
            // B : ATTDIA
            hdrVars.bAttdia = readBit(buf, offset.get(), bitOffset.get(), "ATTDIA", cb);
        }
        
        // B : MIRRTEXT
        hdrVars.bMirrtext = readBit(buf, offset.get(), bitOffset.get(), "MIRRTEXT", cb);
        // B : WORLDVIEW
        hdrVars.bWorldview = readBit(buf, offset.get(), bitOffset.get(), "WORLDVIEW", cb);

        // R13-R14 Only
        // B : WIREFRAME Undocumented.
        if (ver.between(DwgVersion.R13, DwgVersion.R14)) {
            hdrVars.bWireframe = readBit(buf, offset.get(), bitOffset.get(), "WIREFRAME", cb);
        }
        
        // Common
        // B : TILEMODE
        hdrVars.bTilemode = readBit(buf, offset.get(), bitOffset.get(), "TILEMODE", cb);
        // B : PLIMCHECK
        hdrVars.bPlimcheck = readBit(buf, offset.get(), bitOffset.get(), "PLIMCHECK", cb);
        // B : VISRETAIN
        hdrVars.bVisretain = readBit(buf, offset.get(), bitOffset.get(), "VISRETAIN", cb);

        // R13-R14 Only (Stored in registry from R15 onwards)
        // B : DELOBJ
        if (ver.between(DwgVersion.R13, DwgVersion.R14)) {
            hdrVars.bDelobj = readBit(buf, offset.get(), bitOffset.get(), "DELOBJ", cb);
        }
        
        // B : DISPSILH
        hdrVars.bDispsilh = readBit(buf, offset.get(), bitOffset.get(), "DISPSILH", cb);
        // B : PELLIPSE (not present in DXF)
        hdrVars.bPellipse = readBit(buf, offset.get(), bitOffset.get(), "PELLIPSE", cb);
        // BS : PROXYGRAPHICS
        hdrVars.sProxygraphics = readBitShort(buf, offset.get(), bitOffset.get(), "PROXYGRAPHICS", cb);

        // R13-R14 Only (Stored in registry from R15 onwards)
        // BS : DRAGMODE
        if (ver.between(DwgVersion.R13, DwgVersion.R14)) {
            hdrVars.sDragmode = readBitShort(buf, offset.get(), bitOffset.get(), "DRAGMODE", cb);
        }

        // Common
        // BS : TREEDEPTH
        hdrVars.sTreedepth = readBitShort(buf, offset.get(), bitOffset.get(), "TREEDEPTH", cb);
        // BS : LUNITS
        hdrVars.sLunits = readBitShort(buf, offset.get(), bitOffset.get(), "LUNITS", cb);
        // BS : LUPREC
        hdrVars.sLuprec = readBitShort(buf, offset.get(), bitOffset.get(), "LUPREC", cb);
        // BS : AUNITS
        hdrVars.sAunits = readBitShort(buf, offset.get(), bitOffset.get(), "AUNITS", cb);
        // BS : AUPREC
        hdrVars.sAuprec = readBitShort(buf, offset.get(), bitOffset.get(), "AUPREC", cb);

        // R13-R14 Only (Stored in registry from R15 onwards)
        // BS : OSMODE
        if (ver.between(DwgVersion.R13, DwgVersion.R14)) {
            hdrVars.sOsmode = readBitShort(buf, offset.get(), bitOffset.get(), "OSMODE", cb);
        }
        
        // Common
        // BS : ATTMODE
        hdrVars.sAttmode = readBitShort(buf, offset.get(), bitOffset.get(), "ATTMODE", cb);

        // R13-R14 Only Only (stored in registry from R15 onwards)
        // BS : COORDS
        if (ver.between(DwgVersion.R13, DwgVersion.R14)) {
            hdrVars.sCoords = readBitShort(buf, offset.get(), bitOffset.get(), "COORDS", cb);
        }

        // Common
        // BS : PDMODE
        hdrVars.sPdmode = readBitShort(buf, offset.get(), bitOffset.get(), "PDMODE", cb);

        // R13-R14 Only Only (stored in registry from R15 onwards)
        // BS : PICKSTYLE
        if (ver.between(DwgVersion.R13, DwgVersion.R14)) {
            hdrVars.sPickstyle = readBitShort(buf, offset.get(), bitOffset.get(), "PICKSTYLE", cb);
        }

        // R2004+
        if (ver.from(DwgVersion.R2004)) {
        	// BL : Unknown
            lUnknown = readBitLong(buf, offset.get(), bitOffset.get(), "Unknown", cb);
            // BL: Unknown
            lUnknown = readBitLong(buf, offset.get(), bitOffset.get(), "Unknown", cb);
            // BL : Unknown
            lUnknown = readBitLong(buf, offset.get(), bitOffset.get(), "Unknown", cb);
        }

        // Common
        // BS : USERI1
        hdrVars.sUseri1 = readBitShort(buf, offset.get(), bitOffset.get(), "USERI1", cb);
        // BS : USERI2
        hdrVars.sUseri2 = readBitShort(buf, offset.get(), bitOffset.get(), "USERI2", cb);
        // BS : USERI3
        hdrVars.sUseri3 = readBitShort(buf, offset.get(), bitOffset.get(), "USERI3", cb);
        // BS : USERI4
        hdrVars.sUseri4 = readBitShort(buf, offset.get(), bitOffset.get(), "USERI4", cb);
        // BS : USERI5
        hdrVars.sUseri5 = readBitShort(buf, offset.get(), bitOffset.get(), "USERI5", cb);
        // BS : SPLINESEGS
        hdrVars.sSplinesegs = readBitShort(buf, offset.get(), bitOffset.get(), "SPLINESEGS", cb);
        // BS : SURFU
        hdrVars.sSurfu = readBitShort(buf, offset.get(), bitOffset.get(), "SURFU", cb);
        // BS : SURFV
        hdrVars.sSurfv = readBitShort(buf, offset.get(), bitOffset.get(), "SURFV", cb);
        // BS : SURFTYPE
        hdrVars.sSurftype = readBitShort(buf, offset.get(), bitOffset.get(), "SURFTYPE", cb);
        // BS : SURFTAB1
        hdrVars.sSurftab1 = readBitShort(buf, offset.get(), bitOffset.get(), "SURFTAB1", cb);
        // BS : SURFTAB2
        hdrVars.sSurftab2 = readBitShort(buf, offset.get(), bitOffset.get(), "SURFTAB2", cb);
        // BS : SPLINETYPE
        hdrVars.sSplinetype = readBitShort(buf, offset.get(), bitOffset.get(), "SPLINETYPE", cb);
        // BS : SHADEDGE
        hdrVars.sShadedge = readBitShort(buf, offset.get(), bitOffset.get(), "SHADEDGE", cb);
        // BS : SHADEDIF
        hdrVars.sShadedif = readBitShort(buf, offset.get(), bitOffset.get(), "SHADEDIF", cb);
        // BS : UNITMODE
        hdrVars.sUnitmode = readBitShort(buf, offset.get(), bitOffset.get(), "UNITMODE", cb);
        // BS : MAXACTVP
        hdrVars.sMaxactvp = readBitShort(buf, offset.get(), bitOffset.get(), "MAXACTVP", cb);
        // BS : ISOLINES
        hdrVars.sIsolines = readBitShort(buf, offset.get(), bitOffset.get(), "ISOLINES", cb);
        // BS : CMLJUST
        hdrVars.sCmljust = readBitShort(buf, offset.get(), bitOffset.get(), "CMLJUST", cb);
        // BS : TEXTQLTY
        hdrVars.sTextqlty = readBitShort(buf, offset.get(), bitOffset.get(), "TEXTQLTY", cb);
        // BD : LTSCALE
        hdrVars.dLtscale = readBitDouble(buf, offset.get(), bitOffset.get(), "LTSCALE", cb);
        // BD : TEXTSIZE
        hdrVars.dTextsize = readBitDouble(buf, offset.get(), bitOffset.get(), "TEXTSIZE", cb);
        // BD : TRACEWID
        hdrVars.dTracewid = readBitDouble(buf, offset.get(), bitOffset.get(), "TRACEWID", cb);
        // BD : SKETCHINC
        hdrVars.dSketchinc = readBitDouble(buf, offset.get(), bitOffset.get(), "SKETCHINC", cb);
        // BD : FILLETRAD
        hdrVars.dFilletrad = readBitDouble(buf, offset.get(), bitOffset.get(), "FILLETRAD", cb);
        // BD : THICKNESS
        hdrVars.dThickness = readBitDouble(buf, offset.get(), bitOffset.get(), "THICKNESS", cb);
        // BD : ANGBASE
        hdrVars.dAngbase = readBitDouble(buf, offset.get(), bitOffset.get(), "ANGBASE", cb);
        // BD : PDSIZE
        hdrVars.dPdsize = readBitDouble(buf, offset.get(), bitOffset.get(), "PDSIZE", cb);
        // BD : PLINEWID
        hdrVars.dPlinewid = readBitDouble(buf, offset.get(), bitOffset.get(), "PLINEWID", cb);
        // BD : USERR1
        hdrVars.dUserr1 = readBitDouble(buf, offset.get(), bitOffset.get(), "USERR1", cb);
        // BD : USERR2
        hdrVars.dUserr2 = readBitDouble(buf, offset.get(), bitOffset.get(), "USERR2", cb);
        // BD : USERR3
        hdrVars.dUserr3 = readBitDouble(buf, offset.get(), bitOffset.get(), "USERR3", cb);
        // BD : USERR4
        hdrVars.dUserr4 = readBitDouble(buf, offset.get(), bitOffset.get(), "USERR4", cb);
        // BD : USERR5
        hdrVars.dUserr5 = readBitDouble(buf, offset.get(), bitOffset.get(), "USERR5", cb);
        // BD : CHAMFERA
        hdrVars.dChamfera = readBitDouble(buf, offset.get(), bitOffset.get(), "CHAMFERA", cb);
        // BD : CHAMFERB
        hdrVars.dChamferb = readBitDouble(buf, offset.get(), bitOffset.get(), "CHAMFERB", cb);
        // BD : CHAMFERC
        hdrVars.dChamferc = readBitDouble(buf, offset.get(), bitOffset.get(), "CHAMFERC", cb);
        // BD : CHAMFERD
        hdrVars.dChamferd = readBitDouble(buf, offset.get(), bitOffset.get(), "CHAMFERD", cb);
        // BD : FACETRES
        hdrVars.dFacetres = readBitDouble(buf, offset.get(), bitOffset.get(), "FACETRES", cb);
        // BD : CMLSCALE
        hdrVars.dCmlscale = readBitDouble(buf, offset.get(), bitOffset.get(), "CMLSCALE", cb);
        // BD : CELTSCALE
        hdrVars.dCeltscale = readBitDouble(buf, offset.get(), bitOffset.get(), "CELTSCALE", cb);

        // R13-R18
        // TV : MENUNAME
        if (ver.between(DwgVersion.R13, DwgVersion.R18)) {
            hdrVars.tMenuname = readVariableText(buf, offset.get(), bitOffset.get(), ver, "MENUNAME", cb);
        }

        // Common
        // BL : TDCREATE (Julian day)
        hdrVars.lTdcreateJD = readBitLong(buf, offset.get(), bitOffset.get(), "TDCREATE (Julian day)", cb);
        // BL : TDCREATE (Milliseconds into the day)
        hdrVars.lTdcreateMS = readBitLong(buf, offset.get(), bitOffset.get(), "TDCREATE (Milliseconds)", cb);
        // BL : TDUPDATE (Julian day)
        hdrVars.lTdupdateJD = readBitLong(buf, offset.get(), bitOffset.get(), "TDUPDATE (Julian day)", cb);
        // BL : TDUPDATE (Milliseconds into the day)
        hdrVars.lTdupdateMS = readBitLong(buf, offset.get(), bitOffset.get(), "TDUPDATE (Milliseconds)", cb);

        // R2004+
        if (ver.from(DwgVersion.R2004)) {
            // BL : Unknown
            lUnknown = readBitLong(buf, offset.get(), bitOffset.get(), "Unknown", cb);
            // BL : Unknown
            lUnknown = readBitLong(buf, offset.get(), bitOffset.get(), "Unknown", cb);
            // BL : Unknown
            lUnknown = readBitLong(buf, offset.get(), bitOffset.get(), "Unknown", cb);
        }

        // Common
        // BL : TDINDWG (Days)
        hdrVars.lTdindwgD = readBitLong(buf, offset.get(), bitOffset.get(), "TDINDWG (Days)", cb);
        // BL : TDINDWG (Milliseconds into the day)
        hdrVars.lTdindwgMS = readBitLong(buf, offset.get(), bitOffset.get(), "TDINDWG (Milliseconds)", cb);
        // BL : TDUSRTIMER (Days)
        hdrVars.lTdusrtimerD = readBitLong(buf, offset.get(), bitOffset.get(), "TDUSRTIMER (Days)", cb);
        // BL : TDUSRTIMER (Milliseconds into the day)
        hdrVars.lTdusrtimerMS = readBitLong(buf, offset.get(), bitOffset.get(), "TDUSRTIMER (Milliseconds)", cb);
        // CMC : CECOLOR
        hdrVars.cmCecolor = readCmColor(buf, offset.get(), bitOffset.get(), ver, "CECOLOR", cb);
        // H : HANDSEED The next handle, with an 8-bit length specifier preceding the handle bytes
    	hdrVars.hHandseed = readHandle(buf, offset.get(), bitOffset.get(), "HANDSEED", cb);
        // H : CLAYER (hard pointer)
    	hdrVars.hClayer = readHandle(buf, offset.get(), bitOffset.get(), "CLAYER", cb);
        // H : TEXTSTYLE (hard pointer)
    	hdrVars.hTextstyle = readHandle(buf, offset.get(), bitOffset.get(), "TEXTSTYLE", cb);
        // H : CELTYPE (hard pointer)
    	hdrVars.hCeltype = readHandle(buf, offset.get(), bitOffset.get(), "CELTYPE", cb);

        // R2007+ Only:
        if (ver.from(DwgVersion.R2007)) {
        	// H : CMATERIAL (hard pointer)
        	hdrVars.hCmaterial = readHandle(buf, offset.get(), bitOffset.get(), "CMATERIAL", cb);
        }
        
        // Common
        // H : DIMSTYLE (hard pointer)
    	hdrVars.hDimstyle = readHandle(buf, offset.get(), bitOffset.get(), "DIMSTYLE", cb);
        // H : CMLSTYLE (hard pointer)
    	hdrVars.hCmlstyle = readHandle(buf, offset.get(), bitOffset.get(), "CMLSTYLE", cb);

        // R2000+ Only:
        if (ver.from(DwgVersion.R2000)) {
            // BD : PSVPSCALE
            hdrVars.dPsvpscale = readBitDouble(buf, offset.get(), bitOffset.get(), "PSVPSCALE", cb);
        }
        
        // Common
        // 3BD : INSBASE (PSPACE)
        hdrVars.dInsbasePspace = read3BitDouble(buf, offset.get(), bitOffset.get(), "INSBASE (PSPACE)", cb);
        // 3BD : EXTMIN (PSPACE)
        hdrVars.dExtminPspace = read3BitDouble(buf, offset.get(), bitOffset.get(), "EXTMIN (PSPACE)", cb);
        // 3BD : EXTMAX (PSPACE)
        hdrVars.dExtmaxPspace = read3BitDouble(buf, offset.get(), bitOffset.get(), "EXTMAX (PSPACE)", cb);
        // 2RD : LIMMIN (PSPACE)
        hdrVars.dLimminPspace = read2RawDouble(buf, offset.get(), bitOffset.get(), "LIMMIN", cb);
        // 2RD : LIMMAX (PSPACE)
        hdrVars.dLimmaxPspace = read2RawDouble(buf, offset.get(), bitOffset.get(), "LIMMAX", cb);
        // BD : ELEVATION (PSPACE)
        hdrVars.dElevationPspace = readBitDouble(buf, offset.get(), bitOffset.get(), "ELEVATION (PSPACE)", cb);
        // 3BD : UCSORG (PSPACE)
        hdrVars.dUcsorgPspace = read3BitDouble(buf, offset.get(), bitOffset.get(), "UCSORG (PSPACE)", cb);
        // 3BD : UCSXDIR (PSPACE)
        hdrVars.dUcsxdirPspace = read3BitDouble(buf, offset.get(), bitOffset.get(), "UCSXDIR (PSPACE)", cb);
        // 3BD : UCSYDIR (PSPACE)
        hdrVars.dUcsydirPspace = read3BitDouble(buf, offset.get(), bitOffset.get(), "UCSYDIR (PSPACE)", cb);
        // H : UCSNAME (PSPACE) (hard pointer)
    	hdrVars.hUcsnamePspace = readHandle(buf, offset.get(), bitOffset.get(), "UCSNAME (PSPACE", cb);

        // R2000+ Only:
        if (ver.from(DwgVersion.R2000)) {
        	// H : PUCSORTHOREF (hard pointer)
        	hdrVars.hPucsorthoref = readHandle(buf, offset.get(), bitOffset.get(), "PUCSORTHOREF", cb);
            // BS : PUCSORTHOVIEW
            hdrVars.sPucsorthoview = readBitShort(buf, offset.get(), bitOffset.get(), "PUCSORTHOVIEW", cb);
            // H : PUCSBASE (hard pointer)
        	hdrVars.hPucsbase = readHandle(buf, offset.get(), bitOffset.get(), "PUCSBASE", cb);
            // 3BD : PUCSORGTOP
            hdrVars.dPucsorgtop = read3BitDouble(buf, offset.get(), bitOffset.get(), "PUCSORGTOP", cb);
            // 3BD : PUCSORGBOTTOM
            hdrVars.dPucsorgbottom = read3BitDouble(buf, offset.get(), bitOffset.get(), "PUCSORGBOTTOM", cb);
            // 3BD : PUCSORGLEFT
            hdrVars.dPucsorgleft = read3BitDouble(buf, offset.get(), bitOffset.get(), "PUCSORGLEFT", cb);
            // 3BD : PUCSORGRIGHT
            hdrVars.dPucsorgright = read3BitDouble(buf, offset.get(), bitOffset.get(), "PUCSORGRIGHT", cb);
            // 3BD : PUCSORGFRONT
            hdrVars.dPucsorgfront = read3BitDouble(buf, offset.get(), bitOffset.get(), "PUCSORGFRONT", cb);
            // 3BD : PUCSORGBACK
            hdrVars.dPucsorgback = read3BitDouble(buf, offset.get(), bitOffset.get(), "PUCSORGBACK", cb);
        }
        
        // Common
        // 3BD : INSBASE (MSPACE)
        hdrVars.dInsbaseMspace = read3BitDouble(buf, offset.get(), bitOffset.get(), "INSBASE (MSPACE)", cb);
        // 3BD : EXTMIN (MSPACE)
        hdrVars.dExtminMspace = read3BitDouble(buf, offset.get(), bitOffset.get(), "EXTMIN (MSPACE)", cb);
        // 3BD : EXTMAX (MSPACE)
        hdrVars.dExtmaxMspace = read3BitDouble(buf, offset.get(), bitOffset.get(), "EXTMAX (MSPACE)", cb);
        // 2RD : LIMMIN (MSPACE)
        hdrVars.dLimminPspace = read2RawDouble(buf, offset.get(), bitOffset.get(), "LIMMIN (MSPACE)", cb);
        // 2RD : LIMMAX (MSPACE)
        hdrVars.dLimmaxPspace = read2RawDouble(buf, offset.get(), bitOffset.get(), "LIMMAX (MSPACE)", cb);
        // BD : ELEVATION (MSPACE)
        hdrVars.dElevationMspace = readBitDouble(buf, offset.get(), bitOffset.get(), "ELEVATION (MSPACE)", cb);
        // 3BD : UCSORG (MSPACE)
        hdrVars.dUcsorgMspace = read3BitDouble(buf, offset.get(), bitOffset.get(), "UCSORG (MSPACE)", cb);
        // 3BD : UCSXDIR (MSPACE)
        hdrVars.dUcsxdirMspace = read3BitDouble(buf, offset.get(), bitOffset.get(), "UCSXDIR (MSPACE)", cb);
        // 3BD : UCSYDIR (MSPACE)
        hdrVars.dUcsydirMspace = read3BitDouble(buf, offset.get(), bitOffset.get(), "UCSYDIR (MSPACE)", cb);
        // H : UCSNAME (MSPACE) (hard pointer)
    	hdrVars.hUcsnameMspace = readHandle(buf, offset.get(), bitOffset.get(), "UCSNAME (MSPACE)", cb);

        // R2000+ Only:
        if (ver.from(DwgVersion.R2000)) {
        	// H : UCSORTHOREF (hard pointer)
        	hdrVars.hUcsorthoref = readHandle(buf, offset.get(), bitOffset.get(), "UCSORTHOREF", cb);
            // BS : UCSORTHOVIEW
            hdrVars.sUcsorthoview = readBitShort(buf, offset.get(), bitOffset.get(), "UCSORTHOVIEW", cb);
            // H : UCSBASE (hard pointer)
        	hdrVars.hUcsbase = readHandle(buf, offset.get(), bitOffset.get(), "UCSBASE", cb);
            // 3BD : UCSORGTOP
            hdrVars.dUcsorgtop = read3BitDouble(buf, offset.get(), bitOffset.get(), "UCSORGTOP", cb);
            // 3BD : UCSORGBOTTOM
            hdrVars.dUcsorgbottom = read3BitDouble(buf, offset.get(), bitOffset.get(), "UCSORGBOTTOM", cb);
            // 3BD : UCSORGLEFT
            hdrVars.dUcsorgleft = read3BitDouble(buf, offset.get(), bitOffset.get(), "UCSORGLEFT", cb);
            // 3BD : UCSORGRIGHT
            hdrVars.dUcsorgright = read3BitDouble(buf, offset.get(), bitOffset.get(), "UCSORGRIGHT", cb);
            // 3BD : UCSORGFRONT
            hdrVars.dUcsorgfront = read3BitDouble(buf, offset.get(), bitOffset.get(), "UCSORGFRONT", cb);
            // 3BD : UCSORGBACK
            hdrVars.dUcsorgback = read3BitDouble(buf, offset.get(), bitOffset.get(), "UCSORGBACK", cb);
            // TV : DIMPOST
            hdrVars.tDimpost = readVariableText(buf, offset.get(), bitOffset.get(), ver, "DIMPOST", cb);
            // TV : DIMAPOST
            hdrVars.tDimapost = readVariableText(buf, offset.get(), bitOffset.get(), ver, "DIMAPOST", cb);
        }

        // R13-R14 Only:
        if (ver.between(DwgVersion.R13, DwgVersion.R14)) {
            // B : DIMTOL
            hdrVars.bDimtol = readBit(buf, offset.get(), bitOffset.get(), "DIMTOL", cb);
            // B : DIMLIM
            hdrVars.bDimlim = readBit(buf, offset.get(), bitOffset.get(), "DIMLIM", cb);
            // B : DIMTIH
            hdrVars.bDimtih = readBit(buf, offset.get(), bitOffset.get(), "DIMTIH", cb);
            // B : DIMTOH
            hdrVars.bDimtoh = readBit(buf, offset.get(), bitOffset.get(), "DIMTOH", cb);
            // B : DIMSE1
            hdrVars.bDimse1 = readBit(buf, offset.get(), bitOffset.get(), "DIMSE1", cb);
            // B : DIMSE2
            hdrVars.bDimse2 = readBit(buf, offset.get(), bitOffset.get(), "DIMSE2", cb);
            // B : DIMALT
            hdrVars.bDimalt = readBit(buf, offset.get(), bitOffset.get(), "DIMALT", cb);
            // B : DIMTOFL
            hdrVars.bDimtofl = readBit(buf, offset.get(), bitOffset.get(), "DIMTOFL", cb);
            // B : DIMSAH
            hdrVars.bDimsah = readBit(buf, offset.get(), bitOffset.get(), "DIMSAH", cb);
            // B : DIMTIX
            hdrVars.bDimtix = readBit(buf, offset.get(), bitOffset.get(), "DIMTIX", cb);
            // B : DIMSOXD
            hdrVars.bDimsoxd = readBit(buf, offset.get(), bitOffset.get(), "DIMSOXD", cb);
            // RC : DIMALTD
            hdrVars.cDimaltd = readRawChar(buf, offset.get(), bitOffset.get(), "DIMALTD", cb);
            // RC : DIMZIN
            hdrVars.cDimzin = readRawChar(buf, offset.get(), bitOffset.get(), "DIMZIN", cb);
            // B : DIMSD1
            hdrVars.bDimsd1 = readBit(buf, offset.get(), bitOffset.get(), "DIMSD1", cb);
            // B : DIMSD2
            hdrVars.bDimsd2 = readBit(buf, offset.get(), bitOffset.get(), "DIMSD2", cb);
            // RC : DIMTOLJ
            hdrVars.cDimtolj = readRawChar(buf, offset.get(), bitOffset.get(), "DIMTOLJ", cb);
            // RC : DIMJUST
            hdrVars.cDimjust = readRawChar(buf, offset.get(), bitOffset.get(), "DIMJUST", cb);
            // RC : DIMFIT
            hdrVars.cDimfit = readRawChar(buf, offset.get(), bitOffset.get(), "DIMFIT", cb);
            // B : DIMUPT
            hdrVars.bDimupt = readBit(buf, offset.get(), bitOffset.get(), "DIMUPT", cb);
            // RC : DIMTZIN
            hdrVars.cDimtzin = readRawChar(buf, offset.get(), bitOffset.get(), "DIMTZIN", cb);
            // RC : DIMALTZ
            hdrVars.cDimaltz = readRawChar(buf, offset.get(), bitOffset.get(), "DIMALTZ", cb);
            // RC : DIMALTTZ
            hdrVars.cDimalttz = readRawChar(buf, offset.get(), bitOffset.get(), "DIMALTTZ", cb);
            // RC : DIMTAD
            hdrVars.cDimtad = readRawChar(buf, offset.get(), bitOffset.get(), "DIMTAD", cb);
            // BS : DIMUNIT
            hdrVars.sDimunit = readBitShort(buf, offset.get(), bitOffset.get(), "DIMUNIT", cb);
            // BS : DIMAUNIT
            hdrVars.sDimaunit = readBitShort(buf, offset.get(), bitOffset.get(), "DIMAUNIT", cb);
            // BS : DIMDEC
            hdrVars.sDimdec = readBitShort(buf, offset.get(), bitOffset.get(), "DIMDEC", cb);
            // BS : DIMTDEC
            hdrVars.sDimtdec = readBitShort(buf, offset.get(), bitOffset.get(), "DIMTDEC", cb);
            // BS : DIMALTU
            hdrVars.sDimaltu = readBitShort(buf, offset.get(), bitOffset.get(), "DIMALTU", cb);
            // BS : DIMALTTD
            hdrVars.sDimalttd = readBitShort(buf, offset.get(), bitOffset.get(), "DIMALTTD", cb);
            // H : DIMTXSTY (hard pointer)
        	hdrVars.hDimtxsty = readHandle(buf, offset.get(), bitOffset.get(), "DIMTXSTY", cb);
        }

        // Common
    	// BD : DIMSCALE
        hdrVars.dDimscale = readBitDouble(buf, offset.get(), bitOffset.get(), "DIMSCALE", cb);
        // BD : DIMASZ
        hdrVars.dDimasz = readBitDouble(buf, offset.get(), bitOffset.get(), "DIMASZ", cb);
        // BD : DIMEXO
        hdrVars.dDimexo = readBitDouble(buf, offset.get(), bitOffset.get(), "DIMEXO", cb);
        // BD : DIMDLI
        hdrVars.dDimdli = readBitDouble(buf, offset.get(), bitOffset.get(), "DIMDLI", cb);
        // BD : DIMEXE
        hdrVars.dDimexe = readBitDouble(buf, offset.get(), bitOffset.get(), "DIMEXE", cb);
        // BD : DIMRND
        hdrVars.dDimrnd = readBitDouble(buf, offset.get(), bitOffset.get(), "DIMRND", cb);
        // BD : DIMDLE
        hdrVars.dDimdle = readBitDouble(buf, offset.get(), bitOffset.get(), "DIMDLE", cb);
        // BD : DIMTP
        hdrVars.dDimtp = readBitDouble(buf, offset.get(), bitOffset.get(), "DIMTP", cb);
        // BD : DIMTM
        hdrVars.dDimtm = readBitDouble(buf, offset.get(), bitOffset.get(), "DIMTM", cb);

        // R2007+ Only:
        if (ver.from(DwgVersion.R2007)) {
            // BD : DIMFXL
            hdrVars.dDimfxl = readBitDouble(buf, offset.get(), bitOffset.get(), "DIMFXL", cb);
            // BD : DIMJOGANG
            hdrVars.dDimjogang = readBitDouble(buf, offset.get(), bitOffset.get(), "DIMJOGANG", cb);
            // BS : DIMTFILL
            hdrVars.sDimtfill = readBitShort(buf, offset.get(), bitOffset.get(), "DIMTFILL", cb);
            // CMC : DIMTFILLCLR
            hdrVars.cmDimtfillclr = readCmColor(buf, offset.get(), bitOffset.get(), ver, "DIMTFILLCLR", cb);
        }

        // R2000+ Only:
        if (ver.from(DwgVersion.R2000)) {
            // B : DIMTOL
            hdrVars.bDimtol = readBit(buf, offset.get(), bitOffset.get(), "DIMTOL", cb);
            // B : DIMLIM
            hdrVars.bDimlim = readBit(buf, offset.get(), bitOffset.get(), "DIMLIM", cb);
            // B : DIMTIH
            hdrVars.bDimtih = readBit(buf, offset.get(), bitOffset.get(), "DIMTIH", cb);
            // B : DIMTOH
            hdrVars.bDimtoh = readBit(buf, offset.get(), bitOffset.get(), "DIMTOH", cb);
            // B : DIMSE1
            hdrVars.bDimse1 = readBit(buf, offset.get(), bitOffset.get(), "DIMSE1", cb);
            // B : DIMSE2
            hdrVars.bDimse2 = readBit(buf, offset.get(), bitOffset.get(), "DIMSE2", cb);
            // BS : DIMTAD
            hdrVars.sDimtad = readBitShort(buf, offset.get(), bitOffset.get(), "DIMTAD", cb);
            // BS : DIMZIN
            hdrVars.sDimzin = readBitShort(buf, offset.get(), bitOffset.get(), "DIMZIN", cb);
            // BS : DIMAZIN
            hdrVars.sDimazin = readBitShort(buf, offset.get(), bitOffset.get(), "DIMAZIN", cb);
        }

        // R2007+ Only:
        if (ver.from(DwgVersion.R2007)) {
            // BS : DIMARCSYM
            hdrVars.sDimarcsym = readBitShort(buf, offset.get(), bitOffset.get(), "DIMARCSYM", cb);
        }
        
        // Common
        // BD : DIMTXT
        hdrVars.dDimtxt = readBitDouble(buf, offset.get(), bitOffset.get(), "DIMTXT", cb);
        // BD : DIMCEN
        hdrVars.dDimcen = readBitDouble(buf, offset.get(), bitOffset.get(), "DIMCEN", cb);
        // BD : DIMTSZ
        hdrVars.dDimtsz = readBitDouble(buf, offset.get(), bitOffset.get(), "DIMTSZ", cb);
        // BD : DIMALTF
        hdrVars.dDimaltf = readBitDouble(buf, offset.get(), bitOffset.get(), "DIMALTF", cb);
        // BD : DIMLFAC
        hdrVars.dDimlfac = readBitDouble(buf, offset.get(), bitOffset.get(), "DIMLFAC", cb);
        // BD : DIMTVP
        hdrVars.dDimtvp = readBitDouble(buf, offset.get(), bitOffset.get(), "DIMTVP", cb);
        // BD : DIMTFAC
        hdrVars.dDimtfac = readBitDouble(buf, offset.get(), bitOffset.get(), "DIMTFAC", cb);
        // BD : DIMGAP
        hdrVars.dDimgap = readBitDouble(buf, offset.get(), bitOffset.get(), "DIMGAP", cb);
        
        // R13-R14 Only:
        if (ver.between(DwgVersion.R13, DwgVersion.R14)) {
            // T : DIMPOST
            hdrVars.tDimpost = readText(buf, offset.get(), bitOffset.get(), "DIMPOST", cb);
            // T : DIMAPOST
            hdrVars.tDimapost = readText(buf, offset.get(), bitOffset.get(), "DIMAPOST", cb);
            // T : DIMBLK
            hdrVars.tDimblk = readText(buf, offset.get(), bitOffset.get(), "DIMBLK", cb);
            // T : DIMBLK1
            hdrVars.tDimblk1 = readText(buf, offset.get(), bitOffset.get(), "DIMBLK1", cb);
            // T : DIMBLK2
            hdrVars.tDimblk2 = readText(buf, offset.get(), bitOffset.get(), "DIMBLK2", cb);
        }
        
        // R2000+ Only:
        if (ver.from(DwgVersion.R2000)) {
            // BD : DIMALTRND
            hdrVars.dDimaltrnd = readBitDouble(buf, offset.get(), bitOffset.get(), "DIMALTRND", cb);
            // B : DIMALT
            hdrVars.bDimalt = readBit(buf, offset.get(), bitOffset.get(), "DIMALT", cb);
            // BS : DIMALTD
            hdrVars.sDimaltd = readBitShort(buf, offset.get(), bitOffset.get(), "DIMALTD", cb);
            // B : DIMTOFL
            hdrVars.bDimtofl = readBit(buf, offset.get(), bitOffset.get(), "DIMTOFL", cb);
            // B : DIMSAH
            hdrVars.bDimsah = readBit(buf, offset.get(), bitOffset.get(), "DIMSAH", cb);
            // B : DIMTIX
            hdrVars.bDimtix = readBit(buf, offset.get(), bitOffset.get(), "DIMTIX", cb);
            // B : DIMSOXD
            hdrVars.bDimsoxd = readBit(buf, offset.get(), bitOffset.get(), "DIMSOXD", cb);
        }
        
        // CMC : DIMCLRD
        hdrVars.cmDimclrd = readCmColor(buf, offset.get(), bitOffset.get(), ver, "DIMCLRD", cb);
        // CMC : DIMCLRE
        hdrVars.cmDimclre = readCmColor(buf, offset.get(), bitOffset.get(), ver, "DIMCLRE", cb);
        // CMC : DIMCLRT
        hdrVars.cmDimclrt = readCmColor(buf, offset.get(), bitOffset.get(), ver, "DIMCLRT", cb);

        // R2000+ Only:
        if (ver.from(DwgVersion.R2000)) {
            // BS : DIMADEC
            hdrVars.sDimadec = readBitShort(buf, offset.get(), bitOffset.get(), "DIMADEC", cb);
            // BS : DIMDEC
            hdrVars.sDimdec = readBitShort(buf, offset.get(), bitOffset.get(), "DIMDEC", cb);
            // BS : DIMTDEC
            hdrVars.sDimtdec = readBitShort(buf, offset.get(), bitOffset.get(), "DIMTDEC", cb);
            // BS : DIMALTU
            hdrVars.sDimaltu = readBitShort(buf, offset.get(), bitOffset.get(), "DIMALTU", cb);
            // BS : DIMALTTD
            hdrVars.sDimalttd = readBitShort(buf, offset.get(), bitOffset.get(), "DIMALTTD", cb);
            // BS : DIMAUNIT
            hdrVars.sDimaunit = readBitShort(buf, offset.get(), bitOffset.get(), "DIMAUNIT", cb);
            // BS : DIMFRAC
            hdrVars.sDimfrac = readBitShort(buf, offset.get(), bitOffset.get(), "DIMFRAC", cb);
            // BS : DIMLUNIT
            hdrVars.sDimlunit = readBitShort(buf, offset.get(), bitOffset.get(), "DIMLUNIT", cb);
            // BS : DIMDSEP
            hdrVars.sDimdsep = readBitShort(buf, offset.get(), bitOffset.get(), "DIMDSEP", cb);
            // BS : DIMTMOVE
            hdrVars.sDimtmove = readBitShort(buf, offset.get(), bitOffset.get(), "DIMTMOVE", cb);
            // BS : DIMJUST
            hdrVars.sDimjust = readBitShort(buf, offset.get(), bitOffset.get(), "DIMJUST", cb);
            // B : DIMSD1
            hdrVars.bDimsd1 = readBit(buf, offset.get(), bitOffset.get(), "DIMSD1", cb);
            // B : DIMSD2
            hdrVars.bDimsd2 = readBit(buf, offset.get(), bitOffset.get(), "DIMSD2", cb);
            // BS : DIMTOLJ
            hdrVars.sDimtolj = readBitShort(buf, offset.get(), bitOffset.get(), "DIMTOLJ", cb);
            // BS : DIMTZIN
            hdrVars.sDimtzin = readBitShort(buf, offset.get(), bitOffset.get(), "DIMTZIN", cb);
            // BS : DIMALTZ
            hdrVars.sDimaltz = readBitShort(buf, offset.get(), bitOffset.get(), "DIMALTZ", cb);
            // BS : DIMALTTZ
            hdrVars.sDimalttz = readBitShort(buf, offset.get(), bitOffset.get(), "DIMALTTZ", cb);
            // B : DIMUPT
            hdrVars.bDimupt = readBit(buf, offset.get(), bitOffset.get(), "DIMUPT", cb);
            // BS : DIMATFIT
            hdrVars.sDimatfit = readBitShort(buf, offset.get(), bitOffset.get(), "DIMATFIT", cb);
        }

        // R2007+ Only:
        if (ver.from(DwgVersion.R2007)) {
            // B : DIMFXLON
            hdrVars.bDimfxlon = readBit(buf, offset.get(), bitOffset.get(), "DIMFXLON", cb);
        }
        
        // R2010+ Only:
        if (ver.from(DwgVersion.R2010)) {
            // B : DIMTXTDIRECTION
            hdrVars.bDimtxtdirection = readBit(buf, offset.get(), bitOffset.get(), "DIMTXTDIRECTION", cb);
            // BD : DIMALTMZF
            hdrVars.dDimaltmzf = readBitDouble(buf, offset.get(), bitOffset.get(), "DIMALTMZF", cb);
            // T : DIMALTMZS
            hdrVars.tDimaltmzs = readText(buf, offset.get(), bitOffset.get(), "DIMALTMZS", cb);
            // BD : DIMMZF
            hdrVars.dDimmzf = readBitDouble(buf, offset.get(), bitOffset.get(), "DIMMZF", cb);
            // T : DIMMZS
            hdrVars.tDimmzs = readText(buf, offset.get(), bitOffset.get(), "DIMMZS", cb);
        }
        
        // R2000+ Only:
        if (ver.from(DwgVersion.R2000)) {
            // H : DIMTXSTY (hard pointer)
        	hdrVars.hDimtxsty = readHandle(buf, offset.get(), bitOffset.get(), "DIMTXSTY", cb);
            // H : DIMLDRBLK (hard pointer)
        	hdrVars.hDimldrblk = readHandle(buf, offset.get(), bitOffset.get(), "DIMLDRBLK", cb);
            // H : DIMBLK (hard pointer)
        	hdrVars.hDimblk = readHandle(buf, offset.get(), bitOffset.get(), "DIMBLK", cb);
            // H : DIMBLK1 (hard pointer)
        	hdrVars.hDimblk1 = readHandle(buf, offset.get(), bitOffset.get(), "DIMBLK1", cb);
            // H : DIMBLK2 (hard pointer)
        	hdrVars.hDimblk2 = readHandle(buf, offset.get(), bitOffset.get(), "DIMBLK2", cb);
        }
        
        // R2007+ Only:
        if (ver.from(DwgVersion.R2007)) {
            // H : DIMLTYPE (hard pointer)
        	hdrVars.hDimltype = readHandle(buf, offset.get(), bitOffset.get(), "DIMLTYPE", cb);
            // H : DIMLTEX1 (hard pointer)
        	hdrVars.hDimltex1 = readHandle(buf, offset.get(), bitOffset.get(), "DIMLTEX1", cb);
            // H : DIMLTEX2 (hard pointer)
        	hdrVars.hDimltex2 = readHandle(buf, offset.get(), bitOffset.get(), "DIMLTEX2", cb);
        }
        
        // R2000+ only:
        if (ver.from(DwgVersion.R2000)) {
            // BS : DIMLWD
            hdrVars.sDimlwd = readBitShort(buf, offset.get(), bitOffset.get(), "DIMLWD", cb);
            // BS : DIMLWE
            hdrVars.sDimlwe = readBitShort(buf, offset.get(), bitOffset.get(), "DIMLWE", cb);
        }
        
        // Common
        // H : BLOCK CONTROL OBJECT (hard owner)
    	hdrVars.hBlockCtrlObj = readHandle(buf, offset.get(), bitOffset.get(), "BLOCK CONTROL OBJECT", cb);
        // H : LAYER CONTROL OBJECT (hard owner)
    	hdrVars.hLayerCtrlObj = readHandle(buf, offset.get(), bitOffset.get(), "LAYER CONTROL OBJECT", cb);
        // H : STYLE CONTROL OBJECT (hard owner)
    	hdrVars.hStyleCtrlObj = readHandle(buf, offset.get(), bitOffset.get(), "STYLE CONTROL OBJECT", cb);
        // H : LINETYPE CONTROL OBJECT (hard owner)
    	hdrVars.hLinetypeCtrlObj = readHandle(buf, offset.get(), bitOffset.get(), "LINETYPE CONTROL OBJECT", cb);
        // H : VIEW CONTROL OBJECT (hard owner)
    	hdrVars.hViewCtrlObj = readHandle(buf, offset.get(), bitOffset.get(), "VIEW CONTROL OBJECT", cb);
        // H : UCS CONTROL OBJECT (hard owner)
    	hdrVars.hUcsCtrlObj = readHandle(buf, offset.get(), bitOffset.get(), "UCS CONTROL OBJECT", cb);
        // H : VPORT CONTROL OBJECT (hard owner)
    	hdrVars.hVportCtrlObj = readHandle(buf, offset.get(), bitOffset.get(), "VPORT CONTROL OBJECT", cb);
        // H : APPID CONTROL OBJECT (hard owner)
    	hdrVars.hAppidCtrlObj = readHandle(buf, offset.get(), bitOffset.get(), "APPID CONTROL OBJECT", cb);
        // H : DIMSTYLE CONTROL OBJECT (hard owner)
    	hdrVars.hDimstyleCtrlObj = readHandle(buf, offset.get(), bitOffset.get(), "DIMSTYLE CONTROL OBJECT", cb);
            
        // R13-R15 Only:
        if (ver.between(DwgVersion.R13, DwgVersion.R15)) {
        	// H : VIEWPORT ENTITY HEADER CONTROL OBJECT (hard owner)
        	hdrVars.hViewportEttyHdrCtrlObj = readHandle(buf, offset.get(), bitOffset.get(), "VIEWPORT ENTITY HEADER CONTROL OBJECT", cb);
        }
            
        // Common
        // H : DICTIONARY (ACAD_GROUP) (hard pointer)
    	hdrVars.hDictionaryAcadGroup = readHandle(buf, offset.get(), bitOffset.get(), "DICTIONARY (ACAD_GROUP)", cb);
        // H : DICTIONARY (ACAD_MLINESTYLE) (hard pointer)
    	hdrVars.hDictionaryAcadMlinestyle = readHandle(buf, offset.get(), bitOffset.get(), "DICTIONARY (ACAD_MLINESTYLE)", cb);
        // H : DICTIONARY (NAMED OBJECTS) (hard owner)
    	hdrVars.hDictionaryNamedObjs = readHandle(buf, offset.get(), bitOffset.get(), "DICTIONARY (NAMED OBJECTS)", cb);

        // R2000+ Only:
        if (ver.from(DwgVersion.R2000)) {
            // BS : TSTACKALIGN, default = 1 (not present in DXF)
            hdrVars.sTstackalign = readBitShort(buf, offset.get(), bitOffset.get(), "TSTACKALIGN", cb);
        	// BS : TSTACKSIZE, default = 70 (not present in DXF)
        	hdrVars.sTstacksize = readBitShort(buf, offset.get(), bitOffset.get(), "TSTACKSIZE", cb);
            // TV : HYPERLINKBASE
            hdrVars.tHyperlinkbase = readVariableText(buf, offset.get(), bitOffset.get(), ver, "HYPERLINKBASE", cb);
            // TV : STYLESHEET
            hdrVars.tStylesheet = readVariableText(buf, offset.get(), bitOffset.get(), ver, "STYLESHEET", cb);
            // H : DICTIONARY (LAYOUTS) (hard pointer)
        	hdrVars.hDictionaryLayouts = readHandle(buf, offset.get(), bitOffset.get(), "DICTIONARY (LAYOUTS)", cb);
            // H : DICTIONARY (PLOTSETTINGS) (hard pointer)
        	hdrVars.hDictionaryPlotsettings = readHandle(buf, offset.get(), bitOffset.get(), "DICTIONARY (PLOTSETTINGS)", cb);
            // H : DICTIONARY (PLOTSTYLES) (hard pointer)
        	hdrVars.hDictionaryPlotstyles = readHandle(buf, offset.get(), bitOffset.get(), "DICTIONARY (PLOTSTYLES)", cb);
        }

        // R2004+:
        if (ver.from(DwgVersion.R2004)) {
        	// H : DICTIONARY (MATERIALS) (hard pointer)
        	hdrVars.hDictionaryMaterials = readHandle(buf, offset.get(), bitOffset.get(), "DICTIONARY (MATERIALS)", cb);
            // H : DICTIONARY (COLORS) (hard pointer)
        	hdrVars.hDictionaryColors = readHandle(buf, offset.get(), bitOffset.get(), "DICTIONARY (COLORS)", cb);
        }
        
        // R2007+:
        if (ver.from(DwgVersion.R2007)) {
        	// H : DICTIONARY (VISUALSTYLE) (hard pointer)
        	hdrVars.hDictionaryVisualstyle = readHandle(buf, offset.get(), bitOffset.get(), "DICTIONARY (VISUALSTYLE)", cb);
        }
        
        HandleRef hUnknown;
        // R2013+:
        if (ver.from(DwgVersion.R2013)) {
        	// H : UNKNOWN (hard pointer)
        	hUnknown = readHandle(buf, offset.get(), bitOffset.get(), "UNKNOWN", cb);
        }
        
        // R2000+
        if (ver.from(DwgVersion.R2000)) {
            // BL : Flags:CELWEIGHT Flags & 0x001F, ENDCAPS Flags & 0x0060, JOINSTYLE Flags & 0x0180, LWDISPLAY !(Flags & 0x0200)
            			//XEDIT !(Flags & 0x0400), EXTNAMES Flags & 0x0800, PSTYLEMODE Flags & 0x2000, OLESTARTUP Flags & 0x4000
	        hdrVars.lFlags = readBitLong(buf, offset.get(), bitOffset.get(), "Flags", cb);
            // BS : INSUNITS
            hdrVars.sInsunits = readBitShort(buf, offset.get(), bitOffset.get(), "INSUNITS", cb);
            // BS : CEPSNTYPE
            hdrVars.sCepsntype = readBitShort(buf, offset.get(), bitOffset.get(), "CEPSNTYPE", cb);
            // H : CPSNID (present only if CEPSNTYPE == 3) (hard pointer)
        	hdrVars.hCpsnid = readHandle(buf, offset.get(), bitOffset.get(), "CPSNID", cb);
            // TV : FINGERPRINTGUID
            hdrVars.tFingerprintguid = readVariableText(buf, offset.get(), bitOffset.get(), ver, "FINGERPRINTGUID", cb);
            // TV : VERSIONGUID
            hdrVars.tVersionguid = readVariableText(buf, offset.get(), bitOffset.get(), ver, "VERSIONGUID", cb);
        }
        
        // R2004+:
        if (ver.from(DwgVersion.R2004)) {
            // RC : SORTENTS
            hdrVars.cSortents = readRawChar(buf, offset.get(), bitOffset.get(), "SORTENTS", cb);
            // RC : INDEXCTL
            hdrVars.cIndexctl = readRawChar(buf, offset.get(), bitOffset.get(), "INDEXCTL", cb);
            // RC : HIDETEXT
            hdrVars.cHidetext = readRawChar(buf, offset.get(), bitOffset.get(), "HIDETEXT", cb);
            // RC : XCLIPFRAME, before R2010 the value can be 0 or 1 only.
            hdrVars.cXclipframe = readRawChar(buf, offset.get(), bitOffset.get(), "XCLIIPFRAME", cb);
            // RC : DIMASSOC
            hdrVars.cDimassoc = readRawChar(buf, offset.get(), bitOffset.get(), "DIMASSOC", cb);
            // RC : HALOGAP
            hdrVars.cHalogap = readRawChar(buf, offset.get(), bitOffset.get(), "HALOGAP", cb);
            // BS : OBSCUREDCOLOR
            hdrVars.sObscuredcolor = readBitShort(buf, offset.get(), bitOffset.get(), "OBSCUREDCOLOR", cb);
            // BS : INTERSECTIONCOLOR
            hdrVars.sIntersectioncolor = readBitShort(buf, offset.get(), bitOffset.get(), "INTERSECTIONCOLOR", cb);
            // RC : OBSCUREDLTYPE
            hdrVars.cObscuredltype = readRawChar(buf, offset.get(), bitOffset.get(), "OBSCUREDLTYPE", cb);
            // RC : INTERSECTIONDISPLAY
            hdrVars.cIntersectiondisplay = readRawChar(buf, offset.get(), bitOffset.get(), "INTERSECTIONDISPLAY", cb);
            // TV : PROJECTNAME
            hdrVars.tProjectname = readVariableText(buf, offset.get(), bitOffset.get(), ver, "PROJECTNAME", cb);
        }
        
        // Common
        // H : BLOCK_RECORD (*PAPER_SPACE) (hard pointer)
    	hdrVars.hBlockRecordPaperSpace = readHandle(buf, offset.get(), bitOffset.get(), "BLOCK_RECORD (*PAPER_SPACE)", cb);
        // H : BLOCK_RECORD (*MODEL_SPACE) (hard pointer)
    	hdrVars.hBlockRecordModelSpace = readHandle(buf, offset.get(), bitOffset.get(), "BLOCK_RECORD (*MODEL_SPACE)", cb);
        // H : LTYPE (BYLAYER) (hard pointer)
    	hdrVars.hLtypeBylayer = readHandle(buf, offset.get(), bitOffset.get(), "LTYPE (BYLAYER)", cb);
        // H : LTYPE (BYBLOCK) (hard pointer)
    	hdrVars.hLtypeByblock = readHandle(buf, offset.get(), bitOffset.get(), "LTYPE (BYBLOCK)", cb);
        // H : LTYPE (CONTINUOUS) (hard pointer)
    	hdrVars.hLtypeContinuous = readHandle(buf, offset.get(), bitOffset.get(), "LTYPE (CONTINUOUS)", cb);
        
        // R2007+
        if (ver.from(DwgVersion.R2007)) {
            // B : CAMERADISPLAY
            hdrVars.bCameradisplay = readBit(buf, offset.get(), bitOffset.get(), "CAMERADISPLAY", cb);
            // BL : unknown
            lUnknown = readBitLong(buf, offset.get(), bitOffset.get(), "Unknown", cb);
            // BL : unknown
            lUnknown = readBitLong(buf, offset.get(), bitOffset.get(), "Unknown", cb);
            // BD : unknown
            dUnknown = readBitDouble(buf, offset.get(), bitOffset.get(), "Unknown", cb);
            // BD : STEPSPERSEC
            hdrVars.dStepspersec = readBitDouble(buf, offset.get(), bitOffset.get(), "STEPSPERSEC", cb);
            // BD : STEPSIZE
            hdrVars.dStepsize = readBitDouble(buf, offset.get(), bitOffset.get(), "STEPSIZE", cb);
            // BD : 3DDWFPREC
            hdrVars.d3ddwfprec = readBitDouble(buf, offset.get(), bitOffset.get(), "3DDWFPREC", cb);
            // BD : LENSLENGTH
            hdrVars.dLenslength = readBitDouble(buf, offset.get(), bitOffset.get(), "LENSLENGTH", cb);
            // BD : CAMERAHEIGHT
            hdrVars.dCameraheight = readBitDouble(buf, offset.get(), bitOffset.get(), "CAMERAHEIGHT", cb);
            // RC : SOLIDHIST
            hdrVars.cSolidhist = readRawChar(buf, offset.get(), bitOffset.get(), "SOLIDHIST", cb);
            // RC : SHOWHIST
            hdrVars.cShowhist = readRawChar(buf, offset.get(), bitOffset.get(), "SHOWHIST", cb);
            // BD : PSOLWIDTH
            hdrVars.dPsolwidth = readBitDouble(buf, offset.get(), bitOffset.get(), "PSOLWIDTH", cb);
            // BD : PSOLHEIGHT
            hdrVars.dPsolheight = readBitDouble(buf, offset.get(), bitOffset.get(), "PSOLHEIGHT", cb);
            // BD : LOFTANG1
            hdrVars.dLoftang1 = readBitDouble(buf, offset.get(), bitOffset.get(), "LOFTANG1", cb);
            // BD : LOFTANG2
            hdrVars.dLoftang2 = readBitDouble(buf, offset.get(), bitOffset.get(), "LOFTANG2", cb);
            // BD : LOFTMAG1
            hdrVars.dLoftmag1 = readBitDouble(buf, offset.get(), bitOffset.get(), "LOFTMAG1", cb);
            // BD : LOFTMAG2
            hdrVars.dLoftmag2 = readBitDouble(buf, offset.get(), bitOffset.get(), "LOFTMAG2", cb);
            // BS : LOFTPARAM
            hdrVars.sLoftparam = readBitShort(buf, offset.get(), bitOffset.get(), "LOFTPARAM", cb);
            // RC : LOFTNORMALS
            hdrVars.cLoftnormals = readRawChar(buf, offset.get(), bitOffset.get(), "LOFTNORMALS", cb);
            // BD : LATITUDE
            hdrVars.dLatitude = readBitDouble(buf, offset.get(), bitOffset.get(), "LATITUDE", cb);
            // BD : LONGITUDE
            hdrVars.dLongitude = readBitDouble(buf, offset.get(), bitOffset.get(), "LONGITUDE", cb);
            // BD : NORTHDIRECTION
            hdrVars.dNorthdirection = readBitDouble(buf, offset.get(), bitOffset.get(), "NORTHDIRECTION", cb);
            // BL : TIMEZONE
            hdrVars.lTimezone = readBitLong(buf, offset.get(), bitOffset.get(), "Unknown", cb);
            // RC : LIGHTGLYPHDISPLAY
            hdrVars.cLightglyphdisplay = readRawChar(buf, offset.get(), bitOffset.get(), "LIGHTGLYPHDISPLAY", cb);
            // RC : TILEMODELIGHTSYNCH
            hdrVars.cTilemodelightsynch = readRawChar(buf, offset.get(), bitOffset.get(), "TILEMODELIGHTSYNCH", cb);
            // RC : DWFFRAME
            hdrVars.cDwfframe = readRawChar(buf, offset.get(), bitOffset.get(), "DWFFRAME", cb);
            // RC : DGNFRAME
            hdrVars.cDgnframe = readRawChar(buf, offset.get(), bitOffset.get(), "DGNFRAME", cb);
            // B : unknown
            boolean bUnknown = readBit(buf, offset.get(), bitOffset.get(), "unknown", cb);
            // CMC : INTERFERECOLOR
            hdrVars.cmInterferecolor = readCmColor(buf, offset.get(), bitOffset.get(), ver, "INTERFERECOLOR", cb);
            // H : INTERFEREOBJVS (hard pointer)
        	hdrVars.hInterfereobjvs = readHandle(buf, offset.get(), bitOffset.get(), "INTERFEREOBJVS", cb);
            // H : INTERFEREVPVS (hard pointer)
        	hdrVars.hInterferevpvs = readHandle(buf, offset.get(), bitOffset.get(), "INTERFEREVPVS", cb);
            // H : DRAGVS (hard pointer)
        	hdrVars.hDragvs = readHandle(buf, offset.get(), bitOffset.get(), "DRAGVS", cb);
            // RC : CSHADOW
            hdrVars.cCshadow = readRawChar(buf, offset.get(), bitOffset.get(), "CSHADOW", cb);
            // BD : unknown
            dUnknown = readBitDouble(buf, offset.get(), bitOffset.get(), "Unknown", cb);
        }
        
        // R14+:
        if (ver.from(DwgVersion.R14)) {
            //BS : unknown short (type 5/6 only) these do not seem to be required,
            sUnknown = readBitShort(buf, offset.get(), bitOffset.get(), "Unknown", cb);
            //BS : unknown short (type 5/6 only) even for type 5.
            sUnknown = readBitShort(buf, offset.get(), bitOffset.get(), "Unknown", cb);
            //BS : unknown short (type 5/6 only)
            sUnknown = readBitShort(buf, offset.get(), bitOffset.get(), "Unknown", cb);
            //BS : unknown short (type 5/6 only)
            sUnknown = readBitShort(buf, offset.get(), bitOffset.get(), "Unknown", cb);
        }
        
        // Common
        // RS : CRC for the data section, starting after the sentinel. Use 0xC0C1 for the initial value.
        short dataSectionCrc = readRawShort(buf, offset.get(), bitOffset.get(), "CRC for the data section", cb);

        // PADDING random bits to the next byte boundary
        if (bitOffset.get()>0) {
            bitOffset.set(0);
            offset.addAndGet(1);
        }

        // Ending sentinel
        // 0x30,0x84,0xE0,0xDC,0x02,0x21,0xC7,0x56,0xA0,0x83,0x97,0x47,0xB1,0x92,0xCC,0xA0
        byte[] compareBytes = new byte[16];
        System.arraycopy(buf, offset.get(), compareBytes, 0, 16);
        offset.addAndGet(16);
        byte[] endingSentinel = {   (byte)0x30,(byte)0x84,(byte)0xE0,(byte)0xDC,(byte)0x02,(byte)0x21,(byte)0xC7,(byte)0x56,
                                    (byte)0xA0,(byte)0x83,(byte)0x97,(byte)0x47,(byte)0xB1,(byte)0x92,(byte)0xCC,(byte)0xA0 };
        if (Arrays.equals(compareBytes, endingSentinel)) {
            log.severe("Ending Sentinel mismatched!!!");
        }                                    
            
        return hdrVars;
    }
    
    private static LocalDateTime fromJulianDay(int lTdcreateDay) {
        double dayMs = Duration.ofDays(1).toMillis();
        LocalDate date = LocalDate.now().with(JulianFields.JULIAN_DAY, (long)lTdcreateDay);
        LocalDateTime result = date.atStartOfDay().plusHours(12).plus((long)(lTdcreateDay%1*mayMs), ChronoUnit.MILLIS);
        log.info("GREGORIAN DATE: " + date);
        return result;
    }

    public static Map<Integer, DrawingClass> readClassSection(byte[] buf, int off, Dwg dwg, DwgVersion ver) {
    	Map<Integer, DrawingClass> drwingClassMap = new HashMap<>();
    	AtomicInteger offset = new AtomicInteger(off);
    	AtomicInteger bitOffset = new AtomicInteger(0);
    	int reBitOffset = 0;
    	
    	if (ver.between(DwgVersion.R13, DwgVersion.R15)) {
    		DecodeCallback cb = new DecodeCallback() {
    			public void onDecoded(String name, Object value, int retBitOffset) {
    				log.info("[" + name+ "] = (" + value.toString() + ")");
    				offset.addAndGet((bitOffset.get()+retBitOffset)/8);
    				bitOffset.set((bitOffset.get()+retBitOffset)%8);
    			}
    		};
    		
    		// beginning sentinel
    		// 0x8D 0xA1 0xC4-xA9 0xF8 0xC5 0xDC 0xF4 0x5F 0xE7 0xCF 0xB6 0x*a
    		offset.addAndGet(16);
    		
    		// size of class date area
    		int sizeClasses = readRawLong(buf, offset.get(), bitOffset.get(), "size of classes data area", cb);
    		
    		// read sets until exhaust the data
    		while(offset.get()-2-16 < sizeClasses) {
    			log.fine("CLASS SECTION read : " + (offset.get()-2-16) + ", Total Size: " + sizeClasses);
    			
    			DrawingClass dc = new DrawingClass();
    			// classnum
    			dc.classnum = readBitShort(buf, offset.get(), bitOffset.get(), "classnum", cb);
    			// version
    			dc.version = readBitShort(buf, offset.get(), bitOffset.get(), "version", cb);
    			// appname
    			dc.appname = readVariableText(buf, offset.get(), bitOffset.get(), ver, "appname", cb);
    			// cplusplusclassname
    			dc.cplusplusclassname = readVariableText(buf, offset.get(), bitOffset.get(), ver, "c++cplusplusclassname", cb);
    			// classdxfname
    			dc.classdxfname = readVariableText(buf, offset.get(), bitOffset.get(), ver, "classdxfname", cb);
    			// wasazombie
    			dc.waszombie = readBit(buf, offset.get(), bitOffset.get(), "c++wasazomebie", cb);
    			// itemclassid
    			dc.itemclassid = readBitShort(buf, offset.get(), bitOffset.get(), "itemclassid", cb);
    			drwingClassMap.put((int)dc.classnum, dc);
    		}
    		
    		// RS: CRC
    		int crc = readBitShort(buf, offset.get(), bitOffset.get(), "CRC", cb);
    		// 16 byte sentinel apprears after the CRC
    		// 0x72,0x5E,0x3B,0x47,0x3B,0x56,0x07,0x3A,0x3F,0x23,0x0B,0xA0,0x180x30,0x49,0x75
    		
    		// PADDING random bits to the next byte boundary
    		if (bitOffset.get()>0) {
    			bitOffset.set(0);;
    			offset.addAndGet(1);
    		}
    		
    		byte[] compareBytes = new byte[16];
    		System.arraycopy(buf,  offset.get(), compareBytes,  0,  16);
    		offset.addAndGet(16);
    		byte[] endingSentinel = {   (byte)0x72,(byte)0x5E,(byte)0x3B,(byte)0x47,(byte)0x3B,(byte)0x56,(byte)0x07,(byte)0x3A,
    									(byte)0x3F,(byte)0x23,(byte)0x0B,(byte)0xA0,(byte)0x18,(byte)0x30,(byte)0x49,(byte)0x75 };
    		if (Arrays.equals(compareBytes, endingSentinel)==false) {
    			log.severe("Ending Sentinel mismatched!!!");
    		}
    	} else if (ver.from(DwgVersion.R18)) {

            try {
                // section is compressed, contains the standard 32byte section header
                byte[] decomBuf = DecoderR2004.decompressR18(buf, offset.get());
                AtomicInteger decomOffset = new AtomicInteger(0);
                
                DecodeCallback cb = new DecodeCallback() {
                    public void onDecoded(String name, Object value, int retBitOffset) {
                        log.info("[" + name+ "] = (" + value.toString() + ")");
                        offset.addAndGet((bitOffset.get()+retBitOffset)/8);
                        bitOffset.set((bitOffset.get()+retBitOffset)%8);
                    }
                };
                
                // beginning sentinel
                // 0x8D 0xA1 0xC4 0xB8 0xC4 0xA9 0xF8 0xC5 0xC0 0xDC 0xF4 0x5F 0xE7 0xCF 0xB6 0x8A
                decomOffset.addAndGet(16);
                
                // size of class date area
                int sizeClasses = readRawLong(buf, decomOffset.get(), bitOffset.get(), "size of classes data area", cb);

                if (ver.from(DwgVersion.R2010)) {
                    // only present if the maintenance is greater than 3!
                    int lUnknown = readRawLong(buf, decomOffset.get(), bitOffset.get(), "size of classes data area", cb);
                }

                if (ver.from(DwgVersion.R2004)) {
                    short maxClassNum = readBitShort(buf, decomOffset.get(), bitOffset.get(), "Maximum class number", cb);
                    byte cUnknonw = readRawChar(buf, decomOffset.get(), bitOffset.get(), "unknown", cb);
                    cUnknonw = readRawChar(buf, decomOffset.get(), bitOffset.get(), "unknown", cb);
                    boolean bUnknown = readBit(buf, decomOffset.get(), bitOffset.get(), "unknown", cb);
                }
                
                // read sets until exhaust the data
                while(offset.get()-2-16 < sizeClasses) {
                    log.fine("CLASS SECTION read : " + (decomOffset.get()-2-16) + ", Total Size: " + sizeClasses);
                    
                    DrawingClass dc = new DrawingClass();
                    // classnum
                    dc.classnum = readBitShort(buf, decomOffset.get(), bitOffset.get(), "classnum", cb);
                    // Proxy falsg 
                    dc.proxyFlags = readBitShort(buf, decomOffset.get(), bitOffset.get(), "proxyFlags", cb);
                    // appname
                    dc.appname = readVariableText(buf, decomOffset.get(), bitOffset.get(), ver, "appname", cb);
                    // cplusplusclassname
                    dc.cplusplusclassname = readVariableText(buf, decomOffset.get(), bitOffset.get(), ver, "c++cplusplusclassname", cb);
                    // classdxfname
                    dc.classdxfname = readVariableText(buf, decomOffset.get(), bitOffset.get(), ver, "classdxfname", cb);
                    // wasazombie
                    dc.waszombie = readBit(buf, decomOffset.get(), bitOffset.get(), "c++wasazomebie", cb);
                    // itemclassid
                    dc.itemclassid = readBitShort(buf, decomOffset.get(), bitOffset.get(), "itemclassid", cb);
                    // Number of objects created of this type in the current DB (DXF 91)
                    dc.numObjCreatedCurDB = readBitLong(buf, decomOffset.get(), bitOffset.get(), "Number of objects created", cb);
                    // Dwg version
                    dc.dwgVersion = readBitShort(buf, decomOffset.get(), bitOffset.get(), "Dwg Version", cb);
                    // Maintenance release version
                    dc.mrVer = readBitShort(buf, decomOffset.get(), bitOffset.get(), "Maintenance release version", cb);
                    //Unknown (normally 0L)
                    int lUnknown = readBitLong(buf, decomOffset.get(), bitOffset.get(), "Unknown", cb);
                    // Unknown (normally 0L)
                    lUnknown = readBitLong(buf, decomOffset.get(), bitOffset.get(), "Unknown", cb);
                    drwingClassMap.put((int)dc.classnum, dc);
                }
			
                // RS: CRC
                int crc = readBitShort(buf, decomOffset.get(), bitOffset.get(), "CRC", cb);
                // 16 byte sentinel apprears after the CRC
                // 0x72,0x5E,0x3B,0x47,0x3B,0x56,0x07,0x3A,0x3F,0x23,0x0B,0xA0,0x180x30,0x49,0x75
                
                // PADDING random bits to the next byte boundary
                if (bitOffset.get()>0) {
                    bitOffset.set(0);;
                    decomOffset.addAndGet(1);
                }
                
                byte[] compareBytes = new byte[16];
                System.arraycopy(buf,  decomOffset.get(), compareBytes,  0,  16);
                decomOffset.addAndGet(16);
                byte[] endingSentinel = { (byte)0x72, (byte)0x5E,(byte)0x3B,(byte)0x47,(byte)0x3B,(byte)0x56,(byte)0x07,(byte)0x3A,
                                            (byte)0x3F,(byte)0x23,(byte)0x0B,(byte)0xA0,(byte)0x18,(byte)0x30,(byte)0x49,(byte)0x75 };
                if (Arrays.equals(compareBytes, endingSentinel)==false) {
                    log.severe("Ending Sentinel mismatched!!!");
                }
            } catch (DwgParseException e) {
                e.printStackTrace();
            }
    	}
    	
    	return drwingClassMap;
    }

    public static int read_AcDb_Handles(byte[] buf, int off, Dwg dwg, DwgVersion ver) {
        AtomicInteger offset = new AtomicInteger(off);
        AtomicInteger bitOffset = new AtomicInteger(0);
        int retBitOffset = 0;

        dwg.data_AcDb_Handles = new HashMap<>();

        // page 251
        if (ver.between(DwgVersion.R13, DwgVersion.R15)) {
            // The Object Map is a table which gives the location of each object in the file This table is broken ito sections.
            // It is basically a list of handle/file loc pairs, and goes (something like) this:
            // Set the "last handle" to all 0 and the "last loc" to 0L;
            // Repeat until section size==2 (the last empty (except the CRC) section)section):
            DecodeCallback cb = new DecodeCallback() {
                public void onDecoded(String name, Object value, int retBitOffset) {
                    log.info("["+name+"] = (" + value.toString() + ")");
                    offset.addAndGet((bitOffset.get()+retBitOffset)/8);
                    bitOffset.set((bitOffset.get()+retBitOffset)%8);
                }
            };

            // Short: size fo this section. note this is in BIGENDIAN order (MSB first)
            short sectionSize = ByteBuffer.wrap(buf, offset.get(), 2).order(ByteOrder.BIG_ENDIAN).getShort();
            offset.addAndGet(2);
            // Repeat until out of data for this section:
            while(offset.get() < sectionSize) {
                long handleOffset = readModularChar(buf, offset.get(), "Handle Offset", cb);
                long locationOffset = readModularChar(buf, offset.get(), "Location Offset", cb);

                dwg.data_AcDb_Handles.put(handleOffset, locationOffset);
                // offset of this handle from last handle as modular char.
                // offset of location in file from last loc as modular char.
                // (note that location offsets can be negative, if the terminating byte has the 4 bit set).
            }
            // End repeat
            // CRC (most significatnt byte followed by least significant byte)
            short CRC = ByteBuffer.wrap(buf, offset.get(), 2).order(ByteOrder.BIG_ENDIAN).getShort();
            offset.addAndGet(2);
            // End of section
            // End top repeat
            // Not that each section is cut off at a maximum length of 2032.
        } else if (ver.DwgVersion.R18)) {
            /* This section is compressed and contains the standard 32 byte section header.
             * The decompressed data in this section is identical to the "Object Map" section data found in R15 and earlier files,
             * excepts that offsets are not absolute file addresses,
             * but are instead offsets into the AcDb:Objects logical section
             * (starting with offset 0 at the beginning of this logical section).
             */
            try {
                byte[] decomBuf = DecoderR2004.decompressR18(buf, offset.get(), 0);
                AtomicInteger decomOffset = new AtomicInteger(0);

                // 32byte section header
                decomOffset.addAndGet(32);

                DecodeCallback cb = new DecodeCallback() {
                    public void onDecoded(String name, Object value, int retBitOffset) {
                        log.info("["+name+"] = (" + value.toString() + ")");
                        offset.addAndGet((bitOffset.get()+retBitOffset)/8);
                        bitOffset.set((bitOffset.get()+retBitOffset)%8);
                    }
                };
    
                short sectionSize = ByteBuffer.wrap(buf, offset.get(), 2).order(ByteOrder.BIG_ENDIAN).getShort();
                decomOffset.addAndGet(2);
                while(decomOffset.get() < sectionSize) {
                    long handleOffset = readModularChar(buf, offset.get(), "Handle Offset", cb);
                    long locationOffset = readModularChar(buf, offset.get(), "Location Offset", cb);
    
                    dwg.data_AcDb_Handles.put(handleOffset, locationOffset);
                }
                short CRC = ByteBuffer.wrap(buf, decomOffset.get(), 2).order(ByteOrder.BIG_ENDIAN).getShort();
                decomOffset.addAndGet(2);
            } catch (DwgParseException e) {
                e.printStackTrace();
            }
        }

        return offset.get()-off;
    }
    
    public static int read_AcDb_Template(byte[] buf, int off, Dwg dwg) {
        int offset = off;

        short len = ByteBuffer.wrap(buf, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        offset += 2;
        dwg.data_AcDb_Tempate = new String(buf, offset, len, StandardCharsets.US_ASCII);
        offset += len;
        int measurement = ByteBuffer.wrap(buf, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        offset += 2;
        log.info("MEASUREMENT :" + measurement);

        return offset-off;
    }

    private static long readModularChar(byte[] buf, int off, String name, DecodeCallback cb) {
        int offset = off;
        byte[] step1Bytes = new byte[8];
        int idx = 0;

        while(true) {
            step1Bytes[idx] = (byte) (buf[offset+idx]);
            if ((step1Bytes[idx]&0x80) == 0x00) {
                break;
            }
            idx += 1;
        }
        idx += 1;

        byte[] step2Bytes = new byte[idx];
        for (int i=0; i<idx; i++) {
            step2Bytes[i] = (byte) ((byte)((step1Bytes[idx-1-i]&0x007F)>>(idx-1-i)) | (i>0 ? ((step1Bytes[idx-i]&0x7F)<<(8-(idx-i))) : 0x00));
        }

        long sum = 0;
        for (int i=0; i<idx; i++) {
            sum += ((step2Bytes[i]&0x00FF)) * (long)Math.pow(256, (idx-1-i)));
        }
        cb.onDecoded(name, sum, (idx+1)*8);

        return sum;
    }

    private static HandleRef readHandle(byte[] buf, int off, int bitOff, String name, DecodeCallback cb) throws DwgParseException {
        HandleRef handleRef = new HandleRef();

        int retBitOffset = 0;
        int offset = off;
        byte refLeft = (byte) ((((buf[offset+1]&0x00FF)>>(8-bitOff))&0xFF) | (buf[offset]<<bitOff&0xFF));
        handleRef.code = (byte)(refLeft>>4 & 0x0F);
        handleRef.counter = (byte)(refLeft & 0x0F);
        handleRef.handles = new byte[handleRef.counter];
        offset += 1; retBitOffset += 8;

        for (int i=0; i<handleRef.counter; i++) {
            handleRef.handles[i] = (byte)((((buf[offset+i+1]&0x00FF)>>(8-bitOff))&0xFF) | (buf[offset+i]<<bitOff&0xFF));
        }
        offset += handleRef.counter;
        retBitOffset += (handleRef.counter*8);
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
                dBuf[i] = (byte) ((((buf[offset+i+1]&0x00FF)>>(8-bitOffset))&0xFF) | (buf[offset+i]<<bitOffset&0xFF));
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
            bitControl = bitOffset==7 ? (byte)((buf[offset]<<1 &0x02)|(buf[offset+1]>>7 & 0x01)) : (byte)(buf[offset]>>(8-bitOffset-2) & 0x03);
            bitOffset += 2; retBitOffset += 2;
            offset += bitOffset/8;
            bitOffset = bitOffset%8;
            dBuf = new byte[4];
            switch(bitControl) {
            case 0: // A long (4bytes) follow, little-endian order (LSB first)
                for (int i=0; i<4; i++) {
                    dBuf[i] = (byte)((((buf[offset+i+1]&0x00FF)>>(8-bitOffset))&0xFF) | (buf[offset+i]<<bitOffset&0xFF));
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
    
    private static byte readRawChar(byte[] buf, int off, int bitOff, String name, DecodeCallback cb) {
    	int retBitOffset = 0;
    	byte value;
    	
    	value = (byte) ((((buf[off+1]&0x00FF)>>(8-bitOff))&0xFF) | (buf[off]<<bitOff&0xFF));
    	retBitOffset += 8;
    	cb.onDecoded(name, value, retBitOffset);
    	
    	return value;
    }

    private static short readRawShort(byte[] buf, int off, int bitOff, String name, DecodeCallback cb) {
    	int retBitOffset = 0;
    	byte[] dBuf = new byte[2];
    	short value;

    	for (int i=0; i<2; i++) {
    		dBuf[i] = (byte) ((((buf[off+i+1]&0x00FF)>>(8-bitOff))&0xFF) | (buf[off+i]<<bitOff&0xFF));
    	}
    	value = ByteBuffer.wrap(dBuf, 0, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
    	retBitOffset += 16;
    	cb.onDecoded(name, value, retBitOffset);
    	
    	return value;
    }

    private static double readRawDouble(byte[] buf, int off, int bitOff, String name, DecodeCallback cb) {
    	int retBitOffset = 0;
    	byte[] dBuf = new byte[8];
    	double value;

    	for (int i=0; i<8; i++) {
    		dBuf[i] = (byte) ((((buf[off+i+1]&0x00FF)>>(8-bitOff))&0xFF) | (buf[off+i]<<bitOff&0xFF));
    	}
    	long lValue = ByteBuffer.wrap(dBuf, 0, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
    	value = Double.longBitsToDouble(lValue);
    	retBitOffset += 64;
    	cb.onDecoded(name, value, retBitOffset);
    	
    	return value;
    }

    private static double[] read2RawDouble(byte[] buf, int off, int bitOff, String name, DecodeCallback cb) {
    	int retBitOffset = 0;
    	byte[] dBuf = new byte[8];
    	double[] values = new double[2];

    	for (int j=0; j<2; j++) {
	    	for (int i=0; i<8; i++) {
	    		dBuf[i] = (byte) ((((buf[off+i+1]&0x00FF)>>(8-bitOff))&0xFF) | (buf[off+i]<<bitOff&0xFF));
	    	}
	    	long lValue = ByteBuffer.wrap(dBuf, 0, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
	    	values[j] = Double.longBitsToDouble(lValue);
	    	retBitOffset += 64;
    	}
    	cb.onDecoded(name, values, retBitOffset);
    	
    	return values;
    }

    private static int readRawLong(byte[] buf, int off, int bitOff, String name, DecodeCallback cb) {
    	int retBitOffset = 0;
    	int bitOffset = bitOff;
    	byte[] dBuf = new byte[4];
    	int value;

    	for (int i=0; i<4; i++) {
    		dBuf[i] = (byte) ((((buf[off+i+1]&0x00FF)>>(8-bitOff))&0xFF) | (buf[off+i]<<bitOff&0xFF));
    	}
    	value = ByteBuffer.wrap(dBuf, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    	retBitOffset += 32;
    	cb.onDecoded(name, value, retBitOffset);
    	
    	return value;
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

    private static double[] read3BitDouble(byte[] buf, int off, int bitOff, String name, DecodeCallback cb) {
        int retBitOffset = 0;
        int offset = off;
        int bitOffset = bitOff;
        byte[] dBuf = new byte[8];
        double[] values = new double[3];

        for (int j=0; j<3; j++) {
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
	            values[j] = Double.longBitsToDouble(lValue);
	            offset += 8; retBitOffset += 64;
	            break;
	        case 1: // 1.0
	            values[j] = 1.0;
	            break;
	        case 2: // 0.0
	            values[j] = 0.0;
	            break;
	        case 3: // not used
	            break;
	        }
	        
        }
        
        cb.onDecoded(name, values, retBitOffset);
        return values;
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

    public static boolean readBit(byte[] buf, int off, int bitOff, String name, DecodeCallback cb) {
        byte value = (byte) (buf[off]>>(8-bitOff-1) & 0x01);
        cb.onDecoded(name, value, 1);
        if ((value&0x01)==0x01)
            return true;
        else
            return false;
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

        byte bitControl = 0;
        switch(bitOffset) {
        case 6:
            bitControl = (byte)((buf[offset]<<1 & 0x06)|(buf[offset+1]>>7 & 0x01));
            break;
        case 7:
            bitControl = (byte)((buf[offset]<<2 & 0x04)|(buf[offset+1]>>6 & 0x03));
            break;
        default:
            bitControl = (byte)(buf[offset]>>(8-bitOffset-3) & 0x07);
            break;
        }
        bitOffset += 3; retBitOffset += 3;
        offset += bitOffset/8;
        bitOffset = bitOffset%8;

        byte[] dBuf = new byte[8];

        for (int i=bitControl; i<8; i++) {
            dBuf[i] = (byte) ((((buf[offset+i+1]&0x00FF)>>(8-bitOffset))&0xFF) | (buf[offset+i]<<bitOffset&0xFF));
        }
        for (int i=0; i<bitControl; i++) {
            dBuf[i] = (byte) ((((buf[offset+i+1]&0x00FF)>>(8-bitOffset))&0xFF) | (buf[offset+i]<<bitOffset&0xFF));
        }
        value = ByteBuffer.wrap(dBuf, 0, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        offset += bitControl; retBitOffset += (bitControl*8);
        cb.onDecoded(name, value, retBitOffset);

        return value;
    }

    public static void read_UnknownSection(byte[] buf, int i, Dwg dwg, DwgVersion ver) {

    }

    public static void read_SecondFileHeader(byte[] buf, int off, Dwg dwg) {
        AtomicInteger offset = new AtomicInteger(off);
        AtomicInteger bitOffset = new AtomicInteger(0);
        int retBitOffset = 0;

        // beginning sentinel
        // 0xD4,0x7B,0x21,0xCE,0x28,0x93,0x9F,0xBF,0x53,0x24,0x40,0x09,0x12,0x3C,0xAA,0x01
        byte[] compareBytes = new byte[16];
        System.arraycopy(buf, offset.get(), compareBytes, 0, 16);
        byte[] beginningSentinel = {    (byte)0xD4,(byte)0x7B,(byte)0x21,(byte)0xCE,(byte)0x28,(byte)0x93,(byte)0x9F,(byte)0xBF,
                                        (byte)0x53,(byte)0x24,(byte)0x40,(byte)0x09,(byte)0x12,(byte)0x3C,(byte)0xAA,(byte)0x01 };
        if (Arrays.equals(compareBytes, beginningSentinel)==false) {
            log.severe("Beginning Sentinel mismatched!!!");
        }
        offset.addAndGet(16);

        DecodeCallback cb = new DecodeCallback() {
            public void onDecoded(String name, Object value, int retBitOffset) {
                log.info("[" + name + "] = (" + value.toString() + ")");
                offset.addAndGet((bitOffset.get()+retBitOffset)/8);
                bitOffset.set((bitOffset.get()+retBitOffset)%8);
            }
        };

        // Size of the section (a 4 byte long)
        int size = readRawLong(buf, offset.get(), bitOffset.get(), "size of this section", cb);

        // Location of this header (long, loc of start of sentinel).
        int loc = readRawLong(buf, offset.get(), bitOffset.get(), "Location of this header", cb);

        // "AC1012" or "AC1014" for R13 or R14 respectively
        byte ver = readRawChar(buf, offset.get(), bitOffset.get(), "AC1012 or AC1014", cb);

        // RC : 6 0's
        // ...




        // ending sentinel
        // 0x2B,0x84,0xDE,0x31,0xD7,0x6C,0x60,0x40,0xAC,0xDB,0xBF,0xF6,0xED,0xC3,0x55,0xFE
        compareBytes = new byte[16];
        System.arraycopy(buf, offset.get(), compareBytes, 0, 16);
        byte[] endingSentinel = {   (byte)0x2B,(byte)0x84,(byte)0xDE,(byte)0x31,(byte)0xD7,(byte)0x6C,(byte)0x60,(byte)0x40,
                                    (byte)0xAC,(byte)0xDB,(byte)0xBF,(byte)0xF6,(byte)0xED,(byte)0xC3,(byte)0x55,(byte)0xFE };
        if (Arrays.equals(compareBytes, endingSentinel)==false) {
            log.severe("Ending Sentinel mismatched!!!");
        }
    }

    public static void read_AcDb_Preview(byte[] buf, int off, Dwg dwg) {
        AtomicInteger offset = new AtomicInteger(off);
        AtomicInteger bitOffset = new AtomicInteger(0);
        int retBitOffset = 0;

        // beginning sentinel
        // 0x1F,0x25,0x6D,0x07,0xD4,0x36,0x28,0x28,0x9D,0x57,0xCA,0x3F,0x9D,0x44,0x10,0x2B
        byte[] compareBytes = new byte[16];
        System.arraycopy(buf, offset.get(), compareBytes, 0, 16);
        byte[] beginningSentinel = {    (byte)0x1F,(byte)0x25,(byte)0x6D,(byte)0x07,(byte)0xD4,(byte)0x36,(byte)0x28,(byte)0x28,
                                        (byte)0x9D,(byte)0x57,(byte)0xCA,(byte)0x3F,(byte)0x9D,(byte)0x44,(byte)0x10,(byte)0x2B };
        if (Arrays.equals(compareBytes, beginningSentinel)==false) {
            log.severe("Beginning Sentinel mismatched!!!");
        }
        offset.addAndGet(16);

        DecodeCallback cb = new DecodeCallback() {
            public void onDecoded(String name, Object value, int retBitOffset) {
                log.info("[" + name + "] = (" + value.toString() + ")");
                offset.addAndGet((bitOffset.get()+retBitOffset)/8);
                bitOffset.set((bitOffset.get()+retBitOffset)%8);
            }
        };

        // ...




        // ending sentinel
        // 0xE0,0xDA,0x92,0xF8,0x2B,0xC9,0xD7,0xD7,0x62,0xA8,0x35,0xC0,0x62,0xBB,0xEF,0xD4
        compareBytes = new byte[16];
        System.arraycopy(buf, offset.get(), compareBytes, 0, 16);
        byte[] endingSentinel = {   (byte)0xE0,(byte)0xDA,(byte)0x92,(byte)0xF8,(byte)0x2B,(byte)0xC9,(byte)0xD7,(byte)0xD7,
                                    (byte)0x62,(byte)0xA8,(byte)0x35,(byte)0xC0,(byte)0x62,(byte)0xBB,(byte)0xEF,(byte)0xD4 };
        if (Arrays.equals(compareBytes, endingSentinel)==false) {
            log.severe("Ending Sentinel mismatched!!!");
        }
    }

}
