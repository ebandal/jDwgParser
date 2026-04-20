package decode.section;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import structure.DwgVersion;

public final class DecoderClasses {
    private static final Logger log = Logger.getLogger(DecoderClasses.class.getName());

    private static final byte[] START_SENTINEL = {
        (byte)0x8D, (byte)0xA1, (byte)0xC4, (byte)0xB8, (byte)0xC4, (byte)0xA9,
        (byte)0xF8, (byte)0xC5, (byte)0xC0, (byte)0xDC, (byte)0xF4, (byte)0x5F,
        (byte)0xE7, (byte)0xCF, (byte)0xB6, (byte)0x8A
    };

    private static final byte[] END_SENTINEL = {
        (byte)0x72, (byte)0x5E, (byte)0x3B, (byte)0x47, (byte)0x3B, (byte)0x56,
        (byte)0x07, (byte)0x3A, (byte)0x3F, (byte)0x23, (byte)0x0B, (byte)0xA0,
        (byte)0x18, (byte)0x30, (byte)0x49, (byte)0x75
    };

    public static class ClassDef {
        public int classNumber;
        public int version;
        public String appName;
        public String cppName;
        public String dxfName;
        public boolean wasZombie;
        public boolean isEntity;

        @Override
        public String toString() {
            return String.format("ClassDef[%d: %s (%s)]", classNumber, dxfName, cppName);
        }
    }

    public static List<ClassDef> readClasses(byte[] buf, int off, DwgVersion ver) {
        List<ClassDef> classes = new ArrayList<>();
        int offset = off;

        // Validate start sentinel
        for (byte b : START_SENTINEL) {
            if (offset >= buf.length || (buf[offset] & 0xFF) != (b & 0xFF)) {
                log.warning("Classes sentinel mismatch");
                return classes;
            }
            offset++;
        }
        log.fine("Classes start sentinel validated");

        // Section size (RL)
        if (offset + 4 > buf.length) return classes;
        long sectionSize = ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
        offset += 4;

        int startOffset = offset;
        int endOffset = (int) (startOffset + sectionSize);

        if (endOffset > buf.length) endOffset = buf.length;

        // Parse class records until end sentinel or boundary
        while (offset < endOffset - 16) {
            try {
                ClassDef def = parseClassRecord(buf, offset);
                if (def != null) {
                    classes.add(def);
                    offset += estimateClassRecordSize(def);
                } else {
                    break;
                }
            } catch (Exception e) {
                log.fine("Class parsing stopped: " + e.getMessage());
                break;
            }
        }

        log.info("Read " + classes.size() + " class definitions");
        return classes;
    }

    private static ClassDef parseClassRecord(byte[] buf, int off) {
        if (off + 10 > buf.length) return null;

        ClassDef def = new ClassDef();

        // RS classNumber
        def.classNumber = ByteBuffer.wrap(buf, off, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
        off += 2;

        // RS version
        def.version = ByteBuffer.wrap(buf, off, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
        off += 2;

        // T appName
        def.appName = readTString(buf, off);
        off += 2 + def.appName.length();

        // T cppName
        def.cppName = readTString(buf, off);
        off += 2 + def.cppName.length();

        // T dxfName
        def.dxfName = readTString(buf, off);
        off += 2 + def.dxfName.length();

        // B wasZombie
        if (off < buf.length) {
            def.wasZombie = (buf[off] & 0x80) != 0;
            off++;
        }

        // BS isEntity
        if (off + 2 <= buf.length) {
            def.isEntity = ByteBuffer.wrap(buf, off, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() != 0;
            off += 2;
        }

        return def;
    }

    private static String readTString(byte[] buf, int off) {
        if (off + 2 > buf.length) return "";
        int len = ByteBuffer.wrap(buf, off, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
        if (len <= 0 || off + 2 + len > buf.length) return "";
        return new String(buf, off + 2, len);
    }

    private static int estimateClassRecordSize(ClassDef def) {
        return 2 + 2 + (2 + def.appName.length()) + (2 + def.cppName.length())
            + (2 + def.dxfName.length()) + 1 + 2;
    }
}
