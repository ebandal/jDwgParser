package decode;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.logging.Logger;

import decode.util.R2004Lz77;
import decode.section.DecoderClasses;
import decode.section.DecoderHandles;
import decode.section.DecoderHeader;
import decode.section.DecoderObjects;
import structure.Dwg;
import structure.header.FileHeader;
import structure.sectionpage.DataSectionPage;
import structure.sectionpage.SystemSectionPage;
import structure.sectionpage.header.DataSectionPageHeader;
import structure.sectionpage.header.SystemSectionPageHeader;

public class DecoderR2004 {
    private static final Logger log = Logger.getLogger(DecoderR2004.class.getName());

    public static int readFileHeader(byte[] buf, int off, FileHeader hdr) throws DwgCrcMismatchException {
        int offset = off;

        // 5bytes of 0x00
        offset += 5;

        // Maintenance release version
        byte mrVer = (byte)(buf[offset]&0xFF);
        offset += 1;
        log.finest("Maintenance release: " + mrVer);

        // byte 0x00, 0x01, 0x03
        offset += 1;

        // Preview address, point to image page + page header size(0x20)
        hdr.previewSeeker = ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        offset += 4;
        log.finest("Preview address : " + hdr.previewSeeker);

        // Application version (Acad version)
        hdr.appVer = (byte)(buf[offset]&0xFF);
        offset += 1;
        log.finest("App ver : "+ hdr.appVer);

        // Application maintenance release version
        hdr.appMrVer = (byte)(buf[offset]&0xFF);
        offset += 1;
        log.finest("App MR ver : " + hdr.appMrVer);

        // Codepage
        hdr.codePage = ByteBuffer.wrap(buf, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        offset += 2;
        log.finest("DWG Code Page : " + hdr.codePage);

        // 3 0x00 bytes
        offset += 3;

        // Security flag
        hdr.security = ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        offset += 4;
        log.finest("Seucurity : " + hdr.security);
        if ((hdr.security&0x01) == 0x01) {
            log.finest("encrypted data except AcDb:Preview and AcDb:SummaryInfo");
        }
        if ((hdr.security&0x02) == 0x02) {
            log.finest("encrypted properties; AcDb:Preview and AcDb:SummaryInfo");
        }
        if ((hdr.security&0x10) == 0x10) {
            log.finest("sign data");
        }
        if ((hdr.security&0x20) == 0x20) {
            log.finest("add timestamp");
        }

        // unknown long
        offset += 4;

        // Summary Info Address
        hdr.summarySeeker = ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        offset += 4;
        log.finest("Summary Info Address : " + hdr.summarySeeker);

        // VBA Project Address
        hdr.vbaSeeker = ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        offset += 4;
        log.finest("VBA Project Address : " + hdr.vbaSeeker);

        // 0x00000000
        offset += 4;

        // 0x00 bytes
        offset += 0x54;

        byte[] magicNumber = new byte[0x6C];
        int randseed = 1;
        for (int i=0; i<magicNumber.length; i++) {
            randseed *= 0x0343FD;
            randseed += 0x269EC3;
            magicNumber[i] = (byte)(randseed>>0x10 & 0xFF);
        }
        // String hexString IntStream.range(0, magicNumber.length).mapToObj(i -> String.foramt("%02x", magicNumbe[i])).collect(Collectors.joining(" "));
        // log.finest("Magic Number : " + hexString);

        // Entryped Data
        byte[] decBuf = new byte[0x6C];
        for (int i=0; i<0x6C; i++) {
            decBuf[i] = (byte) (buf[offset+i] ^ magicNumber[i]);
        }

        int decOffset = 0;
        hdr.fileIdstring = new String(decBuf, decOffset, 12, StandardCharsets.US_ASCII);
        decOffset += 12;
        log.finest("file ID String(AcFssFcAJMB) : " + hdr.fileIdstring);

        decOffset += 4; // 0x00
        decOffset += 4; // 0x6C
        decOffset += 4; // 0x04

        hdr.rootTreeNodeGap = ByteBuffer.wrap(decBuf, decOffset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        decOffset += 4;
        log.finest("root tree node gap : " + hdr.rootTreeNodeGap);

        hdr.lowermostLeftTreeNodeGap = ByteBuffer.wrap(decBuf, decOffset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        decOffset += 4;
        log.finest("Lowermost left tree node gap : " + hdr.lowermostLeftTreeNodeGap);
    
        hdr.lowermostRightTreeNodeGap = ByteBuffer.wrap(decBuf, decOffset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        decOffset += 4;
        log.finest("Lowermost right tree node gap : " + hdr.lowermostRightTreeNodeGap);

        decOffset += 4; // unknown

        hdr.lastSectionPageId = ByteBuffer.wrap(decBuf, decOffset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        decOffset += 4;
        log.finest("Last section Page Id : " + hdr.lastSectionPageId);

        hdr.lastSectionPageEndAddress = ByteBuffer.wrap(decBuf, decOffset, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        decOffset += 8;
        log.finest("Last section page end address : " + hdr.lastSectionPageEndAddress);

        hdr.secondHeaderDataAddress = ByteBuffer.wrap(decBuf, decOffset, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        decOffset += 8;
        log.finest("Second Header data address : " + hdr.secondHeaderDataAddress);

        hdr.gapAmount = ByteBuffer.wrap(decBuf, decOffset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        decOffset += 4;
        log.finest("Gap amount : " + hdr.gapAmount);

        hdr.sectionPageAmount = ByteBuffer.wrap(decBuf, decOffset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        decOffset += 4;
        log.finest("Section page amount : " + hdr.sectionPageAmount);

        decOffset += 4; // 0x20
        decOffset += 4; // 0x80
        decOffset += 4; // 0x40

        hdr.sectionPageMapId = ByteBuffer.wrap(decBuf, decOffset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        decOffset += 4;
        log.finest("Section page Map Id : " + hdr.sectionPageMapId);

        hdr.sectionPageMapAddress = ByteBuffer.wrap(decBuf, decOffset, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        decOffset += 8;
        log.finest("Section page Map address : " + hdr.sectionPageMapAddress);

        hdr.sectionMapId = ByteBuffer.wrap(decBuf, decOffset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        decOffset += 4;
        log.finest("Section Map Id : " + hdr.sectionMapId);

        hdr.sectionPageArraySize = ByteBuffer.wrap(decBuf, decOffset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        decOffset += 4;
        log.finest("Section page array size : " + hdr.sectionPageArraySize);

        hdr.gapArraySize = ByteBuffer.wrap(decBuf, decOffset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        decOffset += 4;
        log.finest("Gap array size : " + hdr.gapArraySize);

        hdr.crc32 = ByteBuffer.wrap(decBuf, decOffset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        decOffset += 4;
        log.finest("CRC32 : " + hdr.crc32);

        int seed = 0;
        int crc = calculateCRC(buf, seed, hdr);
        if (hdr.crc32!=crc) {
            throw new DwgCrcMismatchException();
        }

        offset += decOffset;

        offset += 0x14; // 0x14 bytes copied from the magic number sequence

        return offset-off;
    }

    private static void decryptSectionPage(byte[] buf, int fileOffset) {
        int secMask = 0x4164536b ^ fileOffset;
        for (int i=0; i<8; i++) {
            int hdr = ByteBuffer.wrap(buf, i*4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            hdr ^= secMask;
            byte[] decrypted = ByteBuffer.allocate(4).putInt(hdr).array();
            System.arraycopy(decrypted, 0, buf, i*4, 4);
        }
    }

    public static int readSectionPage(RandomAccessFile raf, Dwg dwg) throws IOException, DwgParseException {
        int offset = 0;
        byte[] buf = new byte[4];
        byte[] headBuf = new byte[0x20];

        for (int i=0; i<dwg.header.sectionPageAmount; i++) {
            log.finest("iteration : " + i);

            int fileOffset = (int)raf.getFilePointer();
            raf.read(buf, 0, 4);
            offset += 4;
            int sectionPageType = ByteBuffer.wrap(buf, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();

            switch(sectionPageType) {
            case 0x41630e3b:
                log.finest("equals to 0x41630e3b");
                offset += readSystemSectionPage(raf, sectionPageType, dwg);
                break;
            case 0x4163003b:
                log.finest("equals to 0x4164003b");
                offset += readSystemSectionPage(raf, sectionPageType, dwg);
                break;
            default:
                raf.seek(fileOffset);
                raf.read(headBuf, 0, 0x20);
                decryptSectionPage(headBuf, fileOffset);

                sectionPageType = ByteBuffer.wrap(headBuf, 0, 4).order(ByteOrder.BIG_ENDIAN).getInt();
                log.finest("Section page type : " + sectionPageType);
                if (sectionPageType == 0x4163043b) {
                    log.finest("equals to 0x4163043b");
                    offset += readDataSectionPage(raf, dwg, headBuf);
                } else {

                }
                break;
            }

            // 0x20 byte boundary of the raw data stream
            long boundary = (int) raf.getFilePointer();
            if (boundary % 0x20 > 0) {
                int skips = 0x20 - (int)boundary%0x20;
                log.severe("stream boundary : " + boundary + ", need to start from : " + (boundary + skips));
                raf.skipBytes(skips);
                offset += skips;
            }
        }

        return offset;
    }

    public static int readSystemSectionPage(RandomAccessFile raf, int sectionPageType, Dwg dwg) throws IOException {
        int offset = 0;

        SystemSectionPage sectionPage = new SystemSectionPage();
        sectionPage.header = new SystemSectionPageHeader();

        // SECTION PAGE HEADER
        {
            sectionPage.header.type = sectionPageType;

            byte[] buf = new byte[16];
            raf.read(buf, 0, 16);

            // Decompressed size
            sectionPage.header.decompressedSize = ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            offset += 4;
            log.finest("Decompressed size : " + sectionPage.header.decompressedSize);

            // Compressed size
            sectionPage.header.compressedSize = ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            offset += 4;
            log.finest("Compressed size : " + sectionPage.header.compressedSize);

            // Compressed type
            sectionPage.header.compressionType = ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            offset += 4;
            log.finest("Compression type : " + sectionPage.header.compressionType);

            // Section page checksum
            sectionPage.header.checksum = ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            offset += 4;
            log.finest("Section page checksum : " + sectionPage.header.checksum);
        }

        // SECTION PAGE
        {
            byte[] compBuf = new byte[sectionPage.header.compressedSize];
            int readLen = raf.read(compBuf, 0, sectionPage.header.compressedSize);
            offset += readLen;
            sectionPage.compressedData = compBuf;

            if (sectionPage.header.compressionType == 2) {
                sectionPage.decompressedData = R2004Lz77.decompress(compBuf, sectionPage.header.decompressedSize);
            } else {
                sectionPage.decompressedData = compBuf;
            }
            log.info("System section: decompressed " + sectionPage.decompressedData.length
                    + "/" + sectionPage.header.decompressedSize + " bytes");
        }

        if (dwg.systemSectionPageList==null) {
            dwg.systemSectionPageList = new ArrayList<>();
        }
        dwg.systemSectionPageList.add(sectionPage);

        return offset;
    }

    public static int readDataSectionPage(RandomAccessFile raf, Dwg dwg, byte[] buf) throws IOException, DwgParseException {
        int offset = 0;

        if (dwg.dataSectionPageList==null) {
            dwg.dataSectionPageList = new ArrayList<>();
        }

        DataSectionPage sectionPage = new DataSectionPage();
        sectionPage.header = new DataSectionPageHeader();

        // DataSectionPageHeader
        {
            // section page type
            sectionPage.header.type = ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.BIG_ENDIAN).getInt();
            offset += 4;

            // section number
            sectionPage.header.sectionNumber = ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.BIG_ENDIAN).getInt();
            offset += 4;
            log.finest("Section number : " + sectionPage.header.sectionNumber);

            // data size (compressed)
            sectionPage.header.compressDataSize = ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.BIG_ENDIAN).getInt();
            offset += 4;
            log.finest("Data size(compressed) : " + sectionPage.header.compressDataSize);

            // page size (decompressed)
            sectionPage.header.decompressedPageSize = ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.BIG_ENDIAN).getInt();
            offset += 4;
            log.finest("Page size(decompressed) : " + sectionPage.header.decompressedPageSize);

            // start offset (in the decompresed buffer)
            sectionPage.header.startOffset = ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.BIG_ENDIAN).getInt();
            offset += 4;
            log.finest("Start offset : " + sectionPage.header.startOffset);

            // Page header checksum
            sectionPage.header.pageHeaderChecksum = ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.BIG_ENDIAN).getInt();
            offset += 4;
            log.finest("Page header Checksum : " + sectionPage.header.pageHeaderChecksum);

            // Data Checksum
            sectionPage.header.dataChecksum = ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.BIG_ENDIAN).getInt();
            offset += 4;
            log.finest("Data Checksum : " + sectionPage.header.dataChecksum);

            // unknown
            offset += 4;
        }

        // SectionPage
        {
            byte[] compBuf = new byte[sectionPage.header.compressDataSize];
            int readLen = raf.read(compBuf, 0, sectionPage.header.compressDataSize);
            offset += readLen;
            sectionPage.compressedData = compBuf;

            if (sectionPage.header.compressDataSize != sectionPage.header.decompressedPageSize) {
                sectionPage.decompressedData = R2004Lz77.decompress(compBuf, sectionPage.header.decompressedPageSize);
            } else {
                sectionPage.decompressedData = compBuf;
            }
            log.info("Data section " + sectionPage.header.sectionNumber + ": decompressed "
                    + sectionPage.decompressedData.length + "/" + sectionPage.header.decompressedPageSize + " bytes");
        }

        dwg.dataSectionPageList.add(sectionPage);

        return offset;
    }

    private static int calculateCRC(byte[] buf, int seed, FileHeader hdr) {
        // not implemented yet

        return hdr.crc32;
    }

    public static void parseSections(Dwg dwg) {
        if (dwg.systemSectionPageList != null && !dwg.systemSectionPageList.isEmpty()) {
            for (SystemSectionPage section : dwg.systemSectionPageList) {
                if (section.decompressedData != null && section.decompressedData.length > 0) {
                    parseSystemSection(section, dwg);
                }
            }
        }

        if (dwg.dataSectionPageList != null && !dwg.dataSectionPageList.isEmpty()) {
            for (DataSectionPage section : dwg.dataSectionPageList) {
                if (section.decompressedData != null && section.decompressedData.length > 0) {
                    parseDataSection(section, dwg);
                }
            }
        }
    }

    private static void parseSystemSection(SystemSectionPage section, Dwg dwg) {
        byte[] data = section.decompressedData;
        if (data.length < 16) {
            log.fine("System section too small: " + data.length);
            return;
        }

        try {
            // System section typically contains multiple subsections
            // Try to parse as Header section (starts with sentinel)
            if (data[0] == (byte)0xCF) {
                log.fine("Parsing as Header section");
                var headerVars = DecoderHeader.readHeader(data, 0, dwg.header.ver);
                if (headerVars != null && !headerVars.isEmpty()) {
                    dwg.headerVariables = new structure.sectionpage.HeaderVariables();
                    log.info("Header section parsed: " + headerVars.size() + " variables");
                }
            }

            // Try to parse as Classes section (starts with different sentinel)
            if (data[0] == (byte)0x8D) {
                log.fine("Parsing as Classes section");
                var classes = DecoderClasses.readClasses(data, 0, dwg.header.ver);
                if (classes != null && !classes.isEmpty()) {
                    log.info("Classes section parsed: " + classes.size() + " classes");
                }
            }
        } catch (Exception e) {
            log.fine("System section parsing error: " + e.getMessage());
        }
    }

    private static void parseDataSection(DataSectionPage section, Dwg dwg) {
        byte[] data = section.decompressedData;
        if (data.length < 4) {
            log.fine("Data section too small: " + data.length);
            return;
        }

        try {
            // Try to parse as Objects section
            var objects = DecoderObjects.readObjects(data, 0, dwg.header.ver, null);
            if (objects != null && !objects.isEmpty()) {
                log.info("Data section parsed: " + objects.size() + " objects");
            }
        } catch (Exception e) {
            log.fine("Data section parsing error: " + e.getMessage());
        }
    }

}
