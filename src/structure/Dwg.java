package structure;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;

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
            fileHeader.ver = DwgVersion.R2000; break;
        case "AC1018":
            fileHeader.ver = DwgVersion.R2004; break;
        case "AC1021":
            fileHeader.ver = DwgVersion.R2007; break;
        case "AC1024":
            fileHeader.ver = DwgVersion.R2010; break;
        case "AC1027":
            fileHeader.ver = DwgVersion.R2013; break;
        case "AC1032":
            fileHeader.ver = DwgVersion.R2018; break;
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

    public static HeaderVariables readHeaderVariable(byte[] buf, int offset, DwgVersion ver) {
        // TODO Auto-generated method stub
        return null;
    }

    public static int readData(byte[] buf, int offset, Dwg dwg) {
        // TODO Auto-generated method stub
        return 0;
    }
}
