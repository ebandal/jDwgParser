package decode.section;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import structure.DwgVersion;

public final class DecoderHeader {
    private static final Logger log = Logger.getLogger(DecoderHeader.class.getName());

    private static final byte[] START_SENTINEL = {
        (byte)0xCF, (byte)0x7B, (byte)0x1F, (byte)0x23, (byte)0xFD, (byte)0xDE,
        (byte)0x38, (byte)0xA9, (byte)0x5F, (byte)0x7C, (byte)0x68, (byte)0xB8,
        (byte)0x4E, (byte)0x6D, (byte)0x33, (byte)0x5F
    };

    public static Map<String, Object> readHeader(byte[] buf, int off, DwgVersion ver) {
        Map<String, Object> vars = new HashMap<>();
        int offset = off;

        // Validate start sentinel
        for (byte b : START_SENTINEL) {
            if (offset >= buf.length || (buf[offset] & 0xFF) != (b & 0xFF)) {
                log.warning("Header sentinel mismatch at offset " + offset);
                return vars;
            }
            offset++;
        }
        log.fine("Header sentinel validated");

        // Section size (RL = 4 bytes)
        if (offset + 4 > buf.length) return vars;
        long sectionSize = ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
        offset += 4;
        log.fine("Header section size: " + sectionSize);

        // Read common header variables (bit-by-bit)
        int bitOffset = 0;
        try {
            vars.put("ACADVER", readVariableText(buf, offset, bitOffset, ver));
            bitOffset = updateBitOffset(buf, offset, bitOffset, buf, "ACADVER");

            if (ver.from(DwgVersion.R2004)) {
                vars.put("ACADMAINTVER", readBitShort(buf, offset, bitOffset));
                bitOffset += 2;
            }

            // DWGCODEPAGE
            String codepage = readVariableText(buf, offset, bitOffset, ver);
            vars.put("DWGCODEPAGE", codepage);

            log.info("Header read: ACADVER=" + vars.get("ACADVER") + ", CODEPAGE=" + codepage);
        } catch (Exception e) {
            log.warning("Header parsing error: " + e.getMessage());
        }

        return vars;
    }

    private static String readVariableText(byte[] buf, int off, int bitOff, DwgVersion ver) {
        int offset = off;
        int bitOffset = bitOff;

        // Variable text: RS (2 bytes) length + string
        if (offset + 2 > buf.length) return "";

        short len = (short) ByteBuffer.wrap(buf, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        offset += 2;

        if (len <= 0 || offset + len > buf.length) return "";

        byte[] strBuf = new byte[len];
        System.arraycopy(buf, offset, strBuf, 0, len);
        return new String(strBuf);
    }

    private static short readBitShort(byte[] buf, int off, int bitOff) {
        if (off >= buf.length) return 0;
        byte bitControl = (byte) ((buf[off] >> (8 - bitOff - 2)) & 0x03);

        switch (bitControl) {
            case 0: // 2-byte short follows
                if (off + 2 >= buf.length) return 0;
                return (short) ByteBuffer.wrap(buf, off + (bitOff / 8), 2)
                    .order(ByteOrder.LITTLE_ENDIAN).getShort();
            case 1: // 1-byte unsigned
                if (off + 1 >= buf.length) return 0;
                return (short) (buf[off + (bitOff / 8) + 1] & 0xFF);
            case 2: return 0;
            case 3: return 256;
            default: return 0;
        }
    }

    private static int updateBitOffset(byte[] buf, int off, int bitOff, byte[] next, String fieldName) {
        return bitOff + 2;
    }
}
