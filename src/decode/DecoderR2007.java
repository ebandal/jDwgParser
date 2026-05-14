package decode;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

import decode.util.ReedSolomon;
import structure.header.FileHeader;

/**
 * R2007+ DWG file decoder.
 *
 * Header structure (§5.2):
 *   0x00-0x05 : version string (AC1021 / AC1024 / AC1027 / AC1032)
 *   0x06-0x0B : 6 unused bytes
 *   0x0C      : maintenance release version
 *   0x0D      : byte marker
 *   0x0E-0x11 : preview address (RL)
 *   0x12      : application version
 *   0x13      : application maintenance version
 *   0x14-0x15 : codepage (RS)
 *   0x16-0x1F : unknown / reserved
 *   0x20-0x27 : page map offset (LE64) -- unencrypted live data
 *   0x28-0x39 : more live data
 *   0x3A-0x7F : padding
 *   0x80-0x457: Reed-Solomon encoded header (0x3d8 = 984 bytes, 3 RS(255,239) blocks)
 *               decodes to 717 bytes
 */
public final class DecoderR2007 {
    private static final Logger log = Logger.getLogger(DecoderR2007.class.getName());

    public static int readMetaData(byte[] buf, int off, FileHeader hdr) throws DwgParseException {
        int offset = off;

        // 5 unused bytes after version string
        offset += 5;

        // Maintenance release version
        offset += 1;

        // byte marker (0x00, 0x01, 0x03)
        offset += 1;

        // Preview seeker (RL)
        hdr.previewSeeker = ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        offset += 4;
        log.finest("Preview address: " + hdr.previewSeeker);

        // Application version / MR version
        hdr.appVer = buf[offset++];
        hdr.appMrVer = buf[offset++];
        log.finest("App ver: " + hdr.appVer + ", MR: " + hdr.appMrVer);

        // Codepage (RS)
        hdr.codePage = ByteBuffer.wrap(buf, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        offset += 2;
        log.finest("Codepage: " + hdr.codePage);

        // Skip reserved/unknown up to 0x20 (3 bytes + 8-byte security? + 4 bytes)
        // Advance from current position (should be 0x16) to 0x20 = 10 bytes skip
        int skipToPageMap = 0x20 - (offset - off);
        if (skipToPageMap > 0) offset += skipToPageMap;

        // Page map offset (LE64) at file offset 0x20
        hdr.pageMapOffset = ByteBuffer.wrap(buf, offset, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        offset += 8;
        log.info("R2007 page map offset: 0x" + Long.toHexString(hdr.pageMapOffset));

        // Skip remaining live data + padding up to 0x80
        int skipToRs = 0x80 - (offset - off);
        if (skipToRs > 0) offset += skipToRs;

        return offset - off;
    }

    public static int readFileHeader(byte[] buf, int off, FileHeader hdr) throws DwgParseException {
        int offset = off;

        // 0x80..0x457: RS-encoded header (0x3d8 = 984 bytes total, but we only need 765 for 3 blocks)
        int rsLen = 0x3d8;
        if (off + rsLen > buf.length) {
            log.warning("R2007 header truncated: need " + rsLen + " bytes, have " + (buf.length - off));
            return 0;
        }

        byte[] rsData = new byte[rsLen];
        System.arraycopy(buf, offset, rsData, 0, rsLen);

        byte[] decoded = ReedSolomon.decodeR2007Data(rsData);
        if (decoded == null) {
            log.warning("R2007 RS decode failed");
        } else {
            hdr.rs_decoded_header = decoded;
            // Parse fields from decoded 717-byte buffer.
            // Layout (libredwg: decode_r2007.c):
            //   0x00..0x07 : file header size (LE64)  -- header_size
            //   0x08..0x0F : file size (LE64)
            //   0x10..0x17 : pages map crc comp
            //   0x18..0x1F : pages map correction
            //   0x20..0x27 : pages map crc seed
            //   0x28..0x2F : pages map2 offset
            //   0x30..0x37 : pages map2 id
            //   0x38..0x3F : pages map offset
            //   0x40..0x47 : pages map id
            //   0x48..0x4F : header2 offset
            //   0x50..0x57 : pages map size comp
            //   0x58..0x5F : pages map size uncomp
            //   0x60..0x67 : pages amount
            //   0x68..0x6F : pages maxid
            //   0x70..0x77 : unknown1 (0x20)
            //   0x78..0x7F : unknown2 (0x40)
            //   0x80..0x87 : pages map crc
            //   0x88..0x8F : unknown3 (0xf800)
            //   0x90..0x97 : unknown4 (4)
            //   0x98..0x9F : unknown5 (1)
            //   0xA0..0xA7 : sections amount (number of sections)
            //   0xA8..0xAF : sections map crc
            //   0xB0..0xB7 : sections map size comp
            //   0xB8..0xBF : sections map id
            //   0xC0..0xC7 : sections map size uncomp
            //   0xC8..0xCF : sections map crc comp
            //   0xD0..0xD7 : sections map correction
            //   0xD8..0xDF : sections map crc seed
            //   0xE0..0xE7 : stream version (0x60100)
            //   0xE8..0xEF : crc seed
            //   0xF0..0xF7 : crc seed encoded
            //   0xF8..0xFF : random seed
            //   0x100..0x107: header crc64
            long headerSize = ByteBuffer.wrap(decoded, 0x00, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
            hdr.pageMapOffset = ByteBuffer.wrap(decoded, 0x38, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
            hdr.pagesMapId    = ByteBuffer.wrap(decoded, 0x40, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
            hdr.pageMapSize   = ByteBuffer.wrap(decoded, 0x58, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
            hdr.numDescriptions = ByteBuffer.wrap(decoded, 0xA0, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
            hdr.sectionsMapId = ByteBuffer.wrap(decoded, 0xB8, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
            log.info("R2007 RS-decoded: headerSize=" + headerSize
                    + " pageMapOffset=0x" + Long.toHexString(hdr.pageMapOffset)
                    + " pagesMapId=" + hdr.pagesMapId
                    + " sectionsMapId=" + hdr.sectionsMapId
                    + " numSections=" + hdr.numDescriptions);
        }

        offset += rsLen;
        return offset - off;
    }
}
